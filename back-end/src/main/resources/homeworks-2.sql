-- Reading homework from "TAREAS pt cat sunt plecata.pdf" (page 2)
-- Espacio DELE · Unidad 1 · Prueba 1 (comprensión de lectura), Tarea 1
-- Email from Edurne to Luciana + 4 SINGLE_CHOICE questions.
-- The PDF's question 5 (pick one of three photos) is skipped: homework questions
-- don't support images yet.
--
-- READ homework keeps the passage in `instructions` (the front-end renders it as
-- the reading text and repeats it in the graded result dialog).
--
-- Unassigned: no unit_id, no homework_targets. Assign later from /panel.

-- This file is UTF-8; without this psql assumes the shell's locale encoding and
-- stores the Spanish accents as mojibake.
SET client_encoding TO 'UTF8';

BEGIN;

DO $body$
DECLARE
  v_hw uuid;
  v_q  uuid;
BEGIN

  -- =========================================================================
  -- Comprensión de lectura — correo de la Escuela Unamuno de Bilbao
  -- =========================================================================
  INSERT INTO homework_assignments (
      title, instructions, published, format, homework_type, level, sort_order
  ) VALUES (
      'DELE A1 · Comprensión de lectura: el correo de la escuela',
      $i$Lee este correo electrónico de Edurne a Luciana, una estudiante que va a España para hacer un curso de español. Luego, responde las preguntas (1-4) seleccionando la opción correcta (a, b o c).

Hola, Luciana:

Soy Edurne Basagoiti, la directora de la Escuela Unamuno de Bilbao.

Ya tenemos copia de tu pasaporte, lo más importante. Los documentos para hacer el examen DELE se completan aquí, no hay problema.

Algunas cosas que te quiero comentar:

Calendario del curso: 21/03/2025–2/06/2025. ¡El primer día celebramos la llegada de la primavera con una pequeña fiesta!

En tu grupo sois dos estudiantes brasileñas, dos de Suiza, otras dos de Lisboa, un chico de Polonia y otro angoleño. Yo soy vasca, de Bilbao.

El profesor de Cultura y Gastronomía vascas se llama Ibai Iturriondobeitia (a veces los apellidos vascos son largos y algo difíciles de recordar). Ibai enseña algunos temas de historia, deporte y cultura del País Vasco. Además, es un gran cocinero y enseña a preparar platos típicos de nuestra gastronomía en la cocina de la escuela.

Ah, en la misma calle de la escuela hay un centro deportivo para correr, hacer gimnasia, nadar, etc. Y a veces organizan excursiones.

¡Hasta pronto!

Edurne$i$,
      true, 'EXERCISE', 'READ', 'A1', 0
  ) RETURNING id INTO v_hw;

  -- 1 --------------------------------------------------------------------
  INSERT INTO homework_questions (assignment_id, position, kind, prompt)
  VALUES (v_hw, 0, 'SINGLE_CHOICE', 'Edurne escribe este correo a Luciana para…')
  RETURNING id INTO v_q;
  INSERT INTO homework_question_options (question_id, position, label, is_correct) VALUES
      (v_q, 0, 'pedir el documento nacional de identidad.', false),
      (v_q, 1, 'informar de las fechas y alumnos del curso.', true),
      (v_q, 2, 'explicar bien cómo son las clases de vasco.', false);

  -- 2 --------------------------------------------------------------------
  INSERT INTO homework_questions (assignment_id, position, kind, prompt)
  VALUES (v_hw, 1, 'SINGLE_CHOICE', 'En la escuela hay una celebración el…')
  RETURNING id INTO v_q;
  INSERT INTO homework_question_options (question_id, position, label, is_correct) VALUES
      (v_q, 0, 'dos de junio.', false),
      (v_q, 1, 'veintiuno de marzo.', true),
      (v_q, 2, 'cinco de mayo.', false);

  -- 3 --------------------------------------------------------------------
  INSERT INTO homework_questions (assignment_id, position, kind, prompt)
  VALUES (v_hw, 2, 'SINGLE_CHOICE',
          'En la clase de Luciana, la mayoría de los estudiantes hablan…')
  RETURNING id INTO v_q;
  INSERT INTO homework_question_options (question_id, position, label, is_correct) VALUES
      (v_q, 0, 'portugués.', true),
      (v_q, 1, 'sueco.', false),
      (v_q, 2, 'polaco.', false);

  -- 4 --------------------------------------------------------------------
  INSERT INTO homework_questions (assignment_id, position, kind, prompt)
  VALUES (v_hw, 3, 'SINGLE_CHOICE', 'El profesor de Cultura y Gastronomía…')
  RETURNING id INTO v_q;
  INSERT INTO homework_question_options (question_id, position, label, is_correct) VALUES
      (v_q, 0, 'enseña en un centro deportivo.', false),
      (v_q, 1, 'tiene un apellido de origen vasco.', true),
      (v_q, 2, 'solo sabe cocinar comida vasca.', false);

  RAISE NOTICE 'Created homework: %', v_hw;
END
$body$;

COMMIT;

-- Verify
SELECT a.id, a.title, a.format, a.homework_type, a.level, a.unit_id,
       (SELECT COUNT(*) FROM homework_questions q WHERE q.assignment_id = a.id) AS questions
FROM homework_assignments a
WHERE a.title = 'DELE A1 · Comprensión de lectura: el correo de la escuela'
ORDER BY a.created_at;
