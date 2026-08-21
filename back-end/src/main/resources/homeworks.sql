-- C4 homeworks from the last 2 slides of C4.pdf (pages 19-20)
-- 1) MULTI_BLANK — p.19 ex. 5: completa con el interrogativo adecuado
-- 2) MATCHING    — p.19 ex. 6: relaciona las preguntas con las respuestas
-- 3) MULTI_BLANK — p.20 ex. 4: completa con ser o tener
--
-- Unassigned: no unit_id, no homework_targets. Assign later from /panel.

-- This file is UTF-8; without this psql assumes the shell's locale encoding and
-- stores the Spanish accents as mojibake.
SET client_encoding TO 'UTF8';

BEGIN;

DO $body$
DECLARE
  v_hw1 uuid;
  v_hw2 uuid;
  v_hw3 uuid;
BEGIN

  -- =========================================================================
  -- Homework 1 — los interrogativos (p.19, ejercicio 5)
  -- =========================================================================
  INSERT INTO homework_assignments (
      title, instructions, published, format, homework_type, level, sort_order
  ) VALUES (
      'C4 · Los interrogativos',
      'Completa las frases con el interrogativo adecuado. Fíjate en la tilde: las palabras interrogativas siempre la llevan.',
      true, 'EXERCISE', 'GRAMMAR', 'A1', 0
  ) RETURNING id INTO v_hw1;

  INSERT INTO homework_questions (assignment_id, position, kind, prompt, structure_json)
  VALUES (
      v_hw1, 0, 'MULTI_BLANK',
      $p$1. ¿___ años tiene Juan?
2. ¿___ es la profesión de Walter?
3. ¿___ vivís Mercedes y tú?
4. ¿De ___ ciudad eres?
5. ¿___ se llama usted?
6. ¿___ es el número de teléfono de la escuela?$p$,
      $j${
        "blanks": [
          {"acceptedAnswers": ["Cuántos", "Cuantos"]},
          {"acceptedAnswers": ["Cuál", "Cual"]},
          {"acceptedAnswers": ["Dónde", "Donde"]},
          {"acceptedAnswers": ["qué", "que"]},
          {"acceptedAnswers": ["Cómo", "Como"]},
          {"acceptedAnswers": ["Cuál", "Cual"]}
        ]
      }$j$::jsonb
  );

  -- =========================================================================
  -- Homework 2 — preguntas y respuestas (p.19, ejercicio 6)
  -- =========================================================================
  INSERT INTO homework_assignments (
      title, instructions, published, format, homework_type, level, sort_order
  ) VALUES (
      'C4 · Preguntas y respuestas',
      'Relaciona cada pregunta con su respuesta.',
      true, 'EXERCISE', 'GRAMMAR', 'A1', 0
  ) RETURNING id INTO v_hw2;

  INSERT INTO homework_questions (assignment_id, position, kind, prompt, structure_json)
  VALUES (
      v_hw2, 0, 'MATCHING',
      'Relaciona las preguntas con las respuestas.',
      $j${
        "left": [
          {"id": "l1", "label": "¿De dónde eres?"},
          {"id": "l2", "label": "¿Cuál es tu correo?"},
          {"id": "l3", "label": "¿Dónde vives?"},
          {"id": "l4", "label": "¿Cómo te llamas?"},
          {"id": "l5", "label": "¿Cuál es tu número de teléfono?"},
          {"id": "l6", "label": "¿Qué lenguas hablas?"},
          {"id": "l7", "label": "¿Cuál es tu profesión?"},
          {"id": "l8", "label": "¿Cuántos años tienes?"}
        ],
        "right": [
          {"id": "r1", "label": "Treinta y siete."},
          {"id": "r2", "label": "Juan."},
          {"id": "r3", "label": "676 54 32 10."},
          {"id": "r4", "label": "Español, inglés y francés."},
          {"id": "r5", "label": "De Guatemala."},
          {"id": "r6", "label": "Soy médico."},
          {"id": "r7", "label": "juanperez@mail.es"},
          {"id": "r8", "label": "En España."}
        ],
        "pairs": [
          {"leftId": "l1", "rightId": "r5"},
          {"leftId": "l2", "rightId": "r7"},
          {"leftId": "l3", "rightId": "r8"},
          {"leftId": "l4", "rightId": "r2"},
          {"leftId": "l5", "rightId": "r3"},
          {"leftId": "l6", "rightId": "r4"},
          {"leftId": "l7", "rightId": "r6"},
          {"leftId": "l8", "rightId": "r1"}
        ]
      }$j$::jsonb
  );

  -- =========================================================================
  -- Homework 3 — ser o tener (p.20, ejercicio 4)
  -- =========================================================================
  INSERT INTO homework_assignments (
      title, instructions, published, format, homework_type, level, sort_order
  ) VALUES (
      'C4 · Ser o tener',
      'Completa las frases con los verbos ser o tener en la forma correcta. Recuerda: en español la edad se expresa con tener, no con ser.',
      true, 'EXERCISE', 'GRAMMAR', 'A1', 0
  ) RETURNING id INTO v_hw3;

  INSERT INTO homework_questions (assignment_id, position, kind, prompt, structure_json)
  VALUES (
      v_hw3, 0, 'MULTI_BLANK',
      $p$1. Me llamo Patricia y ___ 22 años.
2. ¡Hola! Yo ___ Alfredo, ¿y tú?
3. Mónica no ___ 30 años, ___ 29.
4. Justin y Daniel ___ ingleses.
5. Tú y yo ___ españoles.
6. Marga y Luis ___ una cafetería en el centro.
7. Carolina ___ de Ciudad de México.$p$,
      $j${
        "blanks": [
          {"acceptedAnswers": ["tengo"]},
          {"acceptedAnswers": ["soy"]},
          {"acceptedAnswers": ["tiene"]},
          {"acceptedAnswers": ["tiene"]},
          {"acceptedAnswers": ["son"]},
          {"acceptedAnswers": ["somos"]},
          {"acceptedAnswers": ["tienen"]},
          {"acceptedAnswers": ["es"]}
        ]
      }$j$::jsonb
  );

  RAISE NOTICE 'Created homeworks: % / % / %', v_hw1, v_hw2, v_hw3;
END
$body$;

COMMIT;

-- Verify
SELECT a.id, a.title, a.format, a.homework_type, a.level, a.unit_id,
       (SELECT COUNT(*) FROM homework_questions q WHERE q.assignment_id = a.id) AS questions
FROM homework_assignments a
WHERE a.title LIKE 'C4 · %'
ORDER BY a.created_at;
