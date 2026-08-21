-- Three GRAMMAR homeworks: imperfect subjunctive (fill-in) and como si + ser/estar.
-- Unassigned: no unit_id, no homework_targets. Assign later from /panel.
--
-- Homework 1 is split into three MULTI_BLANK questions (one speaker each) because
-- a single passage would exceed the 20-blank cap (22 conjugated verbs).
-- Both -ra and -se imperfect-subjunctive spellings are accepted. Nosotros forms
-- that take a written accent also accept the unaccented variant.
--
-- This file is UTF-8; without this psql assumes the shell's locale encoding and
-- stores the Spanish accents as mojibake.
SET client_encoding TO 'UTF8';

BEGIN;

DO $body$
DECLARE
  v_hw uuid;
BEGIN

  -- =========================================================================
  -- 1. Subjuntivo imperfecto — la lámpara mágica (MULTI_BLANK)
  -- =========================================================================
  INSERT INTO homework_assignments (
      title, instructions, published, format, homework_type, level, sort_order
  ) VALUES (
      'Subjuntivo imperfecto: la lámpara mágica',
      'Si encontrarais una lámpara mágica, ¿qué le pediríais? Escribe la forma correcta del pretérito imperfecto de subjuntivo. El infinitivo aparece entre paréntesis delante de cada hueco.',
      true, 'EXERCISE', 'GRAMMAR', 'B2', 0
  ) RETURNING id INTO v_hw;

  INSERT INTO homework_questions (assignment_id, position, kind, prompt, structure_json)
  VALUES
  (v_hw, 0, 'MULTI_BLANK',
   $p$Marisa: Yo le pediría que me (dar) ___ mi pareja ideal. Me gustaría que (ser) ___ un hombre simpático, pero sobre todo que (ser) ___ inteligente y que (tener) ___ sentido del humor. Además, preferiría que él (tener) ___ mi edad y que le (interesar) ___ la política y el medio ambiente, como a mí. También me gustaría un hombre que (hacer) ___ deporte y a quien le (gustar) ___ los animales. A mí me encantan los perros. Por último, desearía que (leer) ___ los mismos libros que yo para poder conversar sobre ellos.$p$,
   $j${
     "blanks": [
       {"acceptedAnswers": ["diera", "diese"]},
       {"acceptedAnswers": ["fuera", "fuese"]},
       {"acceptedAnswers": ["fuera", "fuese"]},
       {"acceptedAnswers": ["tuviera", "tuviese"]},
       {"acceptedAnswers": ["tuviera", "tuviese"]},
       {"acceptedAnswers": ["interesara", "interesase", "interesaran", "interesasen"]},
       {"acceptedAnswers": ["hiciera", "hiciese"]},
       {"acceptedAnswers": ["gustaran", "gustasen"]},
       {"acceptedAnswers": ["leyera", "leyese"]}
     ]
   }$j$::jsonb),

  (v_hw, 1, 'MULTI_BLANK',
   $p$Tere: ¡Qué aburrido! Yo quisiera que mi novio (ser) ___ muy guapo y que (tener) ___ muchos músculos y que (practicar) ___ vela. No me importaría que (leer) ___ o no libros, porque a mí no me gusta leer. Eso sí, me gustaría que (ser) ___ inteligente y se (comportar) ___ como un caballero.$p$,
   $j${
     "blanks": [
       {"acceptedAnswers": ["fuera", "fuese"]},
       {"acceptedAnswers": ["tuviera", "tuviese"]},
       {"acceptedAnswers": ["practicara", "practicase"]},
       {"acceptedAnswers": ["leyera", "leyese"]},
       {"acceptedAnswers": ["fuera", "fuese"]},
       {"acceptedAnswers": ["comportara", "comportase"]}
     ]
   }$j$::jsonb),

  (v_hw, 2, 'MULTI_BLANK',
   $p$Luis: A mí no me importaría que mi pareja no (ser) ___ muy inteligente. Pero me gustaría que (ser) ___ una chica alta y delgada. Preferiría que (tener) ___ el pelo largo y que se (vestir) ___ muy moderno. Además, me encantaría que a mi novia le (gustar) ___ el fútbol, (saber) ___ cocinar y que (conducir) ___ muy bien, porque vosotras no sabéis conducir…
Marisa y Tere: ¡Hombres!$p$,
   $j${
     "blanks": [
       {"acceptedAnswers": ["fuera", "fuese"]},
       {"acceptedAnswers": ["fuera", "fuese"]},
       {"acceptedAnswers": ["tuviera", "tuviese"]},
       {"acceptedAnswers": ["vistiera", "vistiese"]},
       {"acceptedAnswers": ["gustara", "gustase"]},
       {"acceptedAnswers": ["supiera", "supiese"]},
       {"acceptedAnswers": ["condujera", "condujese"]}
     ]
   }$j$::jsonb);

  RAISE NOTICE 'Created homework (lámpara mágica): %', v_hw;

  -- =========================================================================
  -- 2. Como si + ser / estar (numbered SINGLE_CHOICE)
  -- =========================================================================
  INSERT INTO homework_assignments (
      title, instructions, published, format, homework_type, level, sort_order
  ) VALUES (
      'Como si + ser / estar',
      'Elige la forma correcta del pretérito imperfecto de subjuntivo: ser o estar, según el contexto de cada frase con «como si».',
      true, 'EXERCISE', 'GRAMMAR', 'B2', 0
  ) RETURNING id INTO v_hw;

  INSERT INTO homework_questions (assignment_id, position, kind, prompt, structure_json)
  VALUES
  (v_hw, 0, 'SINGLE_CHOICE',
   $p$Os comportáis como si (1) niños pequeños.
Escribe como si (2) un novelista clásico.
Nos sentimos jovencísimos, como si (3) adolescentes.
Nos habla como si (4) enfadada con nosotros.
Camina como si (5) más joven de lo que es.
Me hablas como si (6) mi madre.
Ponte cómoda, como si (7) en tu casa.
Comen como si (8) muertos de hambre.
Habla inglés como si (9) nativa.$p$,
   $j${
     "items": [
       {"number": 1, "options": [
         {"id": "7c1a0b12-4e8f-4a91-9d3e-1a2b3c4d5e01", "label": "estuvierais", "correct": false},
         {"id": "7c1a0b12-4e8f-4a91-9d3e-1a2b3c4d5e02", "label": "fuerais", "correct": true}
       ]},
       {"number": 2, "options": [
         {"id": "7c1a0b12-4e8f-4a91-9d3e-1a2b3c4d5e03", "label": "estuviese", "correct": false},
         {"id": "7c1a0b12-4e8f-4a91-9d3e-1a2b3c4d5e04", "label": "fuese", "correct": true}
       ]},
       {"number": 3, "options": [
         {"id": "7c1a0b12-4e8f-4a91-9d3e-1a2b3c4d5e05", "label": "estuviéramos", "correct": false},
         {"id": "7c1a0b12-4e8f-4a91-9d3e-1a2b3c4d5e06", "label": "fuésemos", "correct": true}
       ]},
       {"number": 4, "options": [
         {"id": "7c1a0b12-4e8f-4a91-9d3e-1a2b3c4d5e07", "label": "fuera", "correct": false},
         {"id": "7c1a0b12-4e8f-4a91-9d3e-1a2b3c4d5e08", "label": "estuviera", "correct": true}
       ]},
       {"number": 5, "options": [
         {"id": "7c1a0b12-4e8f-4a91-9d3e-1a2b3c4d5e09", "label": "fuese", "correct": true},
         {"id": "7c1a0b12-4e8f-4a91-9d3e-1a2b3c4d5e0a", "label": "estuviese", "correct": false}
       ]},
       {"number": 6, "options": [
         {"id": "7c1a0b12-4e8f-4a91-9d3e-1a2b3c4d5e0b", "label": "estuvieras", "correct": false},
         {"id": "7c1a0b12-4e8f-4a91-9d3e-1a2b3c4d5e0c", "label": "fueras", "correct": true}
       ]},
       {"number": 7, "options": [
         {"id": "7c1a0b12-4e8f-4a91-9d3e-1a2b3c4d5e0d", "label": "estuvieras", "correct": true},
         {"id": "7c1a0b12-4e8f-4a91-9d3e-1a2b3c4d5e0e", "label": "fueras", "correct": false}
       ]},
       {"number": 8, "options": [
         {"id": "7c1a0b12-4e8f-4a91-9d3e-1a2b3c4d5e0f", "label": "fueran", "correct": false},
         {"id": "7c1a0b12-4e8f-4a91-9d3e-1a2b3c4d5e10", "label": "estuvieran", "correct": true}
       ]},
       {"number": 9, "options": [
         {"id": "7c1a0b12-4e8f-4a91-9d3e-1a2b3c4d5e11", "label": "fuese", "correct": true},
         {"id": "7c1a0b12-4e8f-4a91-9d3e-1a2b3c4d5e12", "label": "estuviese", "correct": false}
       ]}
     ]
   }$j$::jsonb);

  RAISE NOTICE 'Created homework (como si): %', v_hw;

  -- =========================================================================
  -- 3. Subjuntivo imperfecto — viajar (MULTI_BLANK)
  -- =========================================================================
  INSERT INTO homework_assignments (
      title, instructions, published, format, homework_type, level, sort_order
  ) VALUES (
      'Subjuntivo imperfecto: viajar',
      'Completa el texto con el pretérito imperfecto de subjuntivo. El infinitivo aparece entre paréntesis delante de cada hueco.',
      true, 'EXERCISE', 'GRAMMAR', 'B2', 0
  ) RETURNING id INTO v_hw;

  INSERT INTO homework_questions (assignment_id, position, kind, prompt, structure_json)
  VALUES
  (v_hw, 0, 'MULTI_BLANK',
   $p$Cuando era joven, soñaba con poder viajar por todo el mundo sin preocuparme por el dinero ni por el tiempo. Quería que mis amigos me (acompañar) ___, aunque sabía que a veces sería difícil que todos nosotros (tener) ___ los mismos días libres. Esperaba que mi familia me (entender) ___ cuando les decía que viajar no era solo un pasatiempo, sino una forma de aprender y crecer como persona.$p$,
   $j${
     "blanks": [
       {"acceptedAnswers": ["acompañaran", "acompañasen"]},
       {"acceptedAnswers": ["tuviéramos", "tuviésemos", "tuvieramos", "tuviesemos"]},
       {"acceptedAnswers": ["entendiera", "entendiese"]}
     ]
   }$j$::jsonb),

  (v_hw, 1, 'MULTI_BLANK',
   $p$Hice mi primer viaje a los 17 años y lo recuerdo como una de las mejores experiencias de mi juventud. Recuerdo que, durante aquellos días, quería que los días (ser) ___ largos para visitar todos los lugares que me interesaban. Pronto me di cuenta de que, si hablaba mejor otros idiomas, iba a ser más fácil que la gente me (comprender) ___, así que me puse manos a la obra y empecé a aprender inglés, francés y alemán. Sin duda, dominar estos idiomas, además de mi lengua materna, hizo que mis posteriores viajes (convertirse) ___ en visitas mucho más interesantes y completas.$p$,
   $j${
     "blanks": [
       {"acceptedAnswers": ["fueran", "fuesen"]},
       {"acceptedAnswers": ["comprendiera", "comprendiese"]},
       {"acceptedAnswers": ["se convirtieran", "se convirtiesen", "convirtieran", "convirtiesen"]}
     ]
   }$j$::jsonb);

  RAISE NOTICE 'Created homework (viajar): %', v_hw;
END
$body$;

COMMIT;

-- Verify
SELECT a.id, a.title, a.format, a.homework_type, a.level, a.unit_id,
       (SELECT COUNT(*) FROM homework_questions q WHERE q.assignment_id = a.id) AS questions
FROM homework_assignments a
WHERE a.title IN (
    'Subjuntivo imperfecto: la lámpara mágica',
    'Como si + ser / estar',
    'Subjuntivo imperfecto: viajar'
)
ORDER BY a.created_at;
