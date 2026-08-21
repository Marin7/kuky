-- C4 · p.18 — rodee la forma correcta (artículo indeterminado o nada)
-- One question per numbered sentence of the worksheet.
-- Unassigned: no unit_id, no homework_targets. Assign later from /panel.
--
-- This file is UTF-8; without this psql assumes the shell's locale encoding and
-- stores the Spanish accents as mojibake.
SET client_encoding TO 'UTF8';

BEGIN;

DO $body$
DECLARE
  v_hw uuid;
BEGIN

  -- Numbered opción única: every `(N)` in the prompt renders as a clickable
  -- `(forma / forma)` group, so each worksheet sentence is one question and
  -- sentences with two choices contribute two graded items.
  INSERT INTO homework_assignments (
      title, instructions, published, format, homework_type, level, sort_order
  ) VALUES (
      'C4 · ¿Un/una/unos o nada?',
      'Elige la forma correcta en cada caso. Recuerda: con ser no ponemos artículo delante de profesiones, nacionalidades, religiones o ideologías (es profesora, es peruano, son protestantes), pero sí lo ponemos cuando decimos quién es alguien (–¿Quién es? –Es un cantante) y cuando el nombre lleva un adjetivo o un complemento (es una médica buenísima, es un escritor de éxito).',
      true, 'EXERCISE', 'GRAMMAR', 'A1', 0
  ) RETURNING id INTO v_hw;

  INSERT INTO homework_questions (assignment_id, position, kind, prompt, structure_json)
  VALUES
  (v_hw, 0, 'SINGLE_CHOICE', '–¿Quién es Luis Miguel? –Es (1); es (2).',
   $j${
     "items": [
       {"number": 1, "options": [
         {"id": "17b6687f-a3ec-4bb2-b123-3f097a6b798e", "label": "cantante", "correct": false},
         {"id": "cc653b3c-1e97-43e2-983c-b7972cef0dea", "label": "un cantante", "correct": true}
       ]},
       {"number": 2, "options": [
         {"id": "ae5d7880-b524-4802-8e74-bb5e443b7675", "label": "mexicano", "correct": true},
         {"id": "b501f4e7-08b3-441c-9541-a66c546d5ba6", "label": "un mexicano", "correct": false}
       ]}
     ]
   }$j$::jsonb),

  (v_hw, 1, 'SINGLE_CHOICE', 'La doctora Ramírez es (1) buenísima; es (2).',
   $j${
     "items": [
       {"number": 1, "options": [
         {"id": "a5af47e1-a71e-448e-bb1f-15e08e75e417", "label": "médica", "correct": false},
         {"id": "56bf278d-9afd-407b-b9ff-ab1a83ea1229", "label": "una médica", "correct": true}
       ]},
       {"number": 2, "options": [
         {"id": "d7922771-1dc5-4058-94aa-bc04031c847a", "label": "argentina", "correct": true},
         {"id": "de6d53a3-49f7-43df-966f-d8782b5188f8", "label": "una argentina", "correct": false}
       ]}
     ]
   }$j$::jsonb),

  (v_hw, 2, 'SINGLE_CHOICE', 'El marido de Luisa es (1). Es (2) muy caro.',
   $j${
     "items": [
       {"number": 1, "options": [
         {"id": "74dfd6c6-2d36-4bcd-b4c0-ada305db356f", "label": "abogado", "correct": true},
         {"id": "85ba7253-5a0a-40f4-9083-6a94bfe98f37", "label": "un abogado", "correct": false}
       ]},
       {"number": 2, "options": [
         {"id": "627e0021-d5a1-4431-be8d-103686452255", "label": "abogado", "correct": false},
         {"id": "bdcd39e8-32d9-4e01-b685-559b1745b281", "label": "un abogado", "correct": true}
       ]}
     ]
   }$j$::jsonb),

  (v_hw, 3, 'SINGLE_CHOICE', 'Tomás se ha hecho (1).',
   $j${
     "items": [
       {"number": 1, "options": [
         {"id": "5177b10c-c986-4ca9-956a-875231210915", "label": "musulmán", "correct": true},
         {"id": "3c034c92-8ad6-4af0-a346-a7eb1f6c3325", "label": "un musulmán", "correct": false}
       ]}
     ]
   }$j$::jsonb),

  (v_hw, 4, 'SINGLE_CHOICE', 'Los tíos de Andrea son (1).',
   $j${
     "items": [
       {"number": 1, "options": [
         {"id": "096048fd-8ff8-498b-9b86-aa3fcee2bc51", "label": "protestantes", "correct": true},
         {"id": "fd92f093-32f9-48be-9dc0-d83895597552", "label": "unos protestantes", "correct": false}
       ]}
     ]
   }$j$::jsonb),

  (v_hw, 5, 'SINGLE_CHOICE', 'El hermano de Patricio es (1) famoso.',
   $j${
     "items": [
       {"number": 1, "options": [
         {"id": "fb02cfbc-7ad3-4d9a-9048-4fed25091878", "label": "actor", "correct": false},
         {"id": "96f60794-6451-46a0-be26-4b071657cb04", "label": "un actor", "correct": true}
       ]}
     ]
   }$j$::jsonb),

  (v_hw, 6, 'SINGLE_CHOICE', '–¿Quién es Vargas Llosa? –Es (1); es (2).',
   $j${
     "items": [
       {"number": 1, "options": [
         {"id": "f7acbacd-f790-4700-8d3c-2e0c12703862", "label": "escritor", "correct": false},
         {"id": "8a40e3cc-3a27-4944-b5a0-f8ae11b1a504", "label": "un escritor", "correct": true}
       ]},
       {"number": 2, "options": [
         {"id": "4726532c-c3ff-407b-97a8-f2779d0de389", "label": "peruano", "correct": true},
         {"id": "7609bb43-29d0-4c28-91f9-0d100f8f35c0", "label": "un peruano", "correct": false}
       ]}
     ]
   }$j$::jsonb),

  (v_hw, 7, 'SINGLE_CHOICE', 'Alberto y Lola son (1). Son (2).',
   $j${
     "items": [
       {"number": 1, "options": [
         {"id": "e6a8201d-9e5e-4c1f-b58a-c0fdb134e067", "label": "estudiantes", "correct": true},
         {"id": "d10aa1f7-3c05-45d6-a850-f0aa2b412b33", "label": "unos estudiantes", "correct": false}
       ]},
       {"number": 2, "options": [
         {"id": "19c0c930-1a11-489b-9afc-0c38eb60a2e2", "label": "socialistas", "correct": true},
         {"id": "f7d4de80-94d6-43ee-b01f-76904a3d4af0", "label": "unos socialistas", "correct": false}
       ]}
     ]
   }$j$::jsonb),

  (v_hw, 8, 'SINGLE_CHOICE', 'Sarita quiere ser (1).',
   $j${
     "items": [
       {"number": 1, "options": [
         {"id": "15388141-1cd1-4ead-a63d-e90325d33639", "label": "bailarina", "correct": true},
         {"id": "9897e792-9ee4-4789-8ed1-4baff83fc61f", "label": "una bailarina", "correct": false}
       ]}
     ]
   }$j$::jsonb),

  (v_hw, 9, 'SINGLE_CHOICE', 'García Márquez es (1) de éxito.',
   $j${
     "items": [
       {"number": 1, "options": [
         {"id": "d5d68711-d3d0-4af3-a7e3-4f1f1fb58681", "label": "escritor", "correct": false},
         {"id": "ab5807c7-80a3-4187-9f1b-fd1a23ec5f6e", "label": "un escritor", "correct": true}
       ]}
     ]
   }$j$::jsonb);

  RAISE NOTICE 'Created homework: %', v_hw;
END
$body$;

COMMIT;

-- Verify
SELECT a.id, a.title, a.format, a.homework_type, a.level, a.unit_id,
       (SELECT COUNT(*) FROM homework_questions q WHERE q.assignment_id = a.id) AS questions
FROM homework_assignments a
WHERE a.title = 'C4 · ¿Un/una/unos o nada?'
ORDER BY a.created_at;
