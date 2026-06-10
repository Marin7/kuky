-- Resources: teaching materials catalogue
CREATE TABLE resources (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    slug VARCHAR(120) NOT NULL UNIQUE,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    level VARCHAR(8),
    category VARCHAR(80),
    pricing VARCHAR(8) NOT NULL,
    price_cents INT,
    preview_text TEXT,
    related_resource_id UUID REFERENCES resources(id) ON DELETE SET NULL,
    published BOOLEAN NOT NULL DEFAULT true,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT resources_pricing_check CHECK (pricing IN ('FREE', 'PAID')),
    CONSTRAINT resources_price_required_check CHECK (pricing = 'FREE' OR price_cents IS NOT NULL)
);

-- Protected material payloads (files + embeds) for a resource
CREATE TABLE resource_assets (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    resource_id UUID NOT NULL REFERENCES resources(id) ON DELETE CASCADE,
    asset_type VARCHAR(8) NOT NULL,
    label VARCHAR(200) NOT NULL,
    locator TEXT NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    CONSTRAINT resource_assets_type_check CHECK (asset_type IN ('FILE', 'EMBED'))
);

CREATE INDEX resource_assets_resource_id_idx ON resource_assets (resource_id);

-- Bundles: named groups of resources sold at a combined price
CREATE TABLE bundles (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    slug VARCHAR(120) NOT NULL UNIQUE,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    price_cents INT NOT NULL,
    published BOOLEAN NOT NULL DEFAULT true,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Join table: which resources belong to a bundle
CREATE TABLE bundle_resources (
    bundle_id UUID NOT NULL REFERENCES bundles(id) ON DELETE CASCADE,
    resource_id UUID NOT NULL REFERENCES resources(id) ON DELETE CASCADE,
    PRIMARY KEY (bundle_id, resource_id)
);

-- Completed purchase events (financial record + receipt source)
CREATE TABLE purchases (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    item_type VARCHAR(8) NOT NULL,
    resource_id UUID REFERENCES resources(id),
    bundle_id UUID REFERENCES bundles(id),
    amount_cents INT NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'EUR',
    receipt_reference VARCHAR(40) NOT NULL UNIQUE,
    payment_provider VARCHAR(40) NOT NULL,
    payment_reference VARCHAR(120),
    purchased_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT purchases_item_type_check CHECK (item_type IN ('RESOURCE', 'BUNDLE')),
    CONSTRAINT purchases_exactly_one_target CHECK (
        (item_type = 'RESOURCE' AND resource_id IS NOT NULL AND bundle_id IS NULL) OR
        (item_type = 'BUNDLE' AND bundle_id IS NOT NULL AND resource_id IS NULL)
    )
);

CREATE INDEX purchases_user_id_idx ON purchases (user_id);

-- Per-resource access grants (ownership)
CREATE TABLE entitlements (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    resource_id UUID NOT NULL REFERENCES resources(id) ON DELETE CASCADE,
    source_purchase_id UUID NOT NULL REFERENCES purchases(id) ON DELETE CASCADE,
    granted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT entitlements_unique_user_resource UNIQUE (user_id, resource_id)
);

-- -----------------------------------------------------------------------
-- Seed data: placeholder catalogue (clearly labelled, removable later)
-- -----------------------------------------------------------------------

-- Free resource: standalone (no related paid resource)
INSERT INTO resources (slug, title, description, level, category, pricing, preview_text, sort_order)
VALUES (
    'saludos-y-presentaciones',
    'Saludos y presentaciones — muestra gratuita',
    'Una ficha de ejemplo con los saludos más habituales en español.',
    'A1', 'Vocabulario', 'FREE',
    'Aprende a presentarte y a saludar de forma natural. Esta hoja de trabajo cubre las expresiones básicas que todo principiante necesita.',
    1
);

-- Free resource: preview that links to the paid pack-vocabulario-a1
INSERT INTO resources (slug, title, description, level, category, pricing, preview_text, sort_order)
VALUES (
    'vocabulario-a1-preview',
    'Vocabulario A1 — vista previa',
    'Una muestra de 5 fichas del pack completo de vocabulario A1.',
    'A1', 'Vocabulario', 'FREE',
    'Practica las 20 palabras más frecuentes del nivel A1. El pack completo incluye 50 fichas temáticas y audios de pronunciación.',
    2
);

-- Paid resource 1
INSERT INTO resources (slug, title, description, level, category, pricing, price_cents, preview_text, sort_order)
VALUES (
    'pack-vocabulario-a1',
    'Pack de vocabulario A1',
    '50 fichas imprimibles con vocabulario esencial para el nivel A1.',
    'A1', 'Vocabulario', 'PAID', 1500,
    'El pack incluye 50 fichas temáticas (colores, números, familia, comida…), audio de pronunciación y ejercicios de repaso.',
    3
);

-- Paid resource 2
INSERT INTO resources (slug, title, description, level, category, pricing, price_cents, preview_text, sort_order)
VALUES (
    'gramatica-a1',
    'Gramática A1 — guía completa',
    'Guía de gramática esencial para el nivel A1 con ejercicios y soluciones.',
    'A1', 'Gramática', 'PAID', 2500,
    'Cubre los tiempos verbales básicos, los artículos, los pronombres y las preposiciones más usadas. Incluye 30 ejercicios con soluciones.',
    4
);

-- Update the preview to point to its related paid resource
UPDATE resources
SET related_resource_id = (SELECT id FROM resources WHERE slug = 'pack-vocabulario-a1')
WHERE slug = 'vocabulario-a1-preview';

-- Bundle: both paid resources at a discount
INSERT INTO bundles (slug, title, description, price_cents, sort_order)
VALUES (
    'pack-completo-a1',
    'Pack completo A1',
    'Todos los recursos de nivel A1: vocabulario y gramática juntos con descuento.',
    3500,
    1
);

INSERT INTO bundle_resources (bundle_id, resource_id)
SELECT b.id, r.id FROM bundles b, resources r
WHERE b.slug = 'pack-completo-a1' AND r.slug IN ('pack-vocabulario-a1', 'gramatica-a1');

-- Sample assets for the free standalone resource (public, accessible without auth)
INSERT INTO resource_assets (resource_id, asset_type, label, locator, sort_order)
SELECT id, 'EMBED', 'Vídeo introductorio (demo)', 'https://www.youtube.com/embed/placeholder-saludos', 1
FROM resources WHERE slug = 'saludos-y-presentaciones';

INSERT INTO resource_assets (resource_id, asset_type, label, locator, sort_order)
SELECT id, 'FILE', 'Ficha de saludos (PDF demo)', 'https://placeholder.invalid/saludos-fichas.pdf', 2
FROM resources WHERE slug = 'saludos-y-presentaciones';

-- Sample assets for the free preview
INSERT INTO resource_assets (resource_id, asset_type, label, locator, sort_order)
SELECT id, 'FILE', 'Muestra de vocabulario A1 (PDF demo)', 'https://placeholder.invalid/vocabulario-a1-preview.pdf', 1
FROM resources WHERE slug = 'vocabulario-a1-preview';

-- Sample assets for paid resource 1 (gated behind entitlement)
INSERT INTO resource_assets (resource_id, asset_type, label, locator, sort_order)
SELECT id, 'FILE', 'Pack vocabulario A1 — 50 fichas (PDF)', 'https://placeholder.invalid/pack-vocabulario-a1.pdf', 1
FROM resources WHERE slug = 'pack-vocabulario-a1';

INSERT INTO resource_assets (resource_id, asset_type, label, locator, sort_order)
SELECT id, 'EMBED', 'Audios de pronunciación', 'https://placeholder.invalid/embed/pronunciacion-a1', 2
FROM resources WHERE slug = 'pack-vocabulario-a1';

-- Sample assets for paid resource 2 (gated behind entitlement)
INSERT INTO resource_assets (resource_id, asset_type, label, locator, sort_order)
SELECT id, 'FILE', 'Guía de gramática A1 (PDF)', 'https://placeholder.invalid/gramatica-a1.pdf', 1
FROM resources WHERE slug = 'gramatica-a1';

INSERT INTO resource_assets (resource_id, asset_type, label, locator, sort_order)
SELECT id, 'FILE', 'Ejercicios y soluciones A1 (PDF)', 'https://placeholder.invalid/gramatica-a1-ejercicios.pdf', 2
FROM resources WHERE slug = 'gramatica-a1';
