-- C3 homeworks from the last 4 slides of C3.pdf
-- 1) MULTI_BLANK  — Jean's email (ser/estar + bueno/malo / bien/mal)
-- 2) SINGLE_CHOICE + MULTI_BLANK — worksheet 4.1 + 4.2
-- 3) MULTI_BLANK  — Carlos & Ana dialogue (casa de verano)
-- 4) SINGLE_CHOICE — ¿Ser o estar? Esa es la cuestión
--
-- Unassigned: no unit_id, no homework_targets. Assign later from /panel.

BEGIN;

DO $body$
DECLARE
  v_hw1 uuid;
  v_hw2 uuid;
  v_hw3 uuid;
  v_hw4 uuid;
  v_q   uuid;
BEGIN

  -- =========================================================================
  -- Homework 1 — fill in the gaps (Jean's email)
  -- =========================================================================
  INSERT INTO homework_assignments (
      title, instructions, published, format, homework_type, level, sort_order
  ) VALUES (
      'C3 · El correo de Jean (ser / estar)',
      'Jean es un estudiante de Erasmus en Barcelona y confunde bueno/malo y bien/mal con ser y estar. Completa su correo con la forma correcta de ser o estar.',
      true, 'EXERCISE', 'GRAMMAR', 'A1', 0
  ) RETURNING id INTO v_hw1;

  INSERT INTO homework_questions (assignment_id, position, kind, prompt, structure_json)
  VALUES (
      v_hw1, 0, 'MULTI_BLANK',
      $p$De: jean93@uab.edu
Para: vero@bourdeaux.edu

Hola, Verónica:

¿Qué tal todo por Burdeos? Por aquí muy bien, la ciudad ___ muy bien. Ya llevo tres meses en la universidad y los profesores ___ muy buenos, hacen clases muy interesantes. Además, he conocido a mucha gente, entre ellos a una chica muy especial, Chiara, italiana. ___ muy buena, me ayudó desde que llegué a la ciudad a buscar alojamiento y a matricularme en la universidad y me enseñó la ciudad. La conocí en la universidad y el primer día que hablamos ya fuimos a cenar (aquí la comida ___ buenísima). No sé si puedo decir que Chiara y yo somos novios, pero la verdad es que pasamos todo el día juntos y nos lo pasamos muy bien. El tiempo no ___ muy bueno, todo el mes de mayo ha estado lloviendo, pero bueno, no se puede tener todo. Espero que todo vaya bien por allí arriba, ¿Qué tal tus padres?, ¿___ bien? ¿Y tus estudios? ¿Y los novios? Ya me dirás.

Besos,
Jean$p$,
      $j${
        "blanks": [
          {"acceptedAnswers": ["está", "esta"]},
          {"acceptedAnswers": ["son"]},
          {"acceptedAnswers": ["Es", "es"]},
          {"acceptedAnswers": ["está", "esta", "es"]},
          {"acceptedAnswers": ["está", "esta"]},
          {"acceptedAnswers": ["están", "estan"]}
        ]
      }$j$::jsonb
  );

  -- =========================================================================
  -- Homework 2 — 4.1 choose the form + 4.2 fill in the gaps
  -- =========================================================================
  INSERT INTO homework_assignments (
      title, instructions, published, format, homework_type, level, sort_order
  ) VALUES (
      'C3 · Ser o estar (elige y completa)',
      'Primero elige la forma correcta en cada frase. Después completa las frases con la forma adecuada de ser o estar.',
      true, 'EXERCISE', 'GRAMMAR', 'A1', 0
  ) RETURNING id INTO v_hw2;

  -- 4.1 Rodee la forma correcta
  INSERT INTO homework_questions (assignment_id, position, kind, prompt)
  VALUES (v_hw2, 0, 'SINGLE_CHOICE', '1. (Soy / Estoy) cansado.')
  RETURNING id INTO v_q;
  INSERT INTO homework_question_options (question_id, position, label, is_correct) VALUES
    (v_q, 0, 'Soy', false),
    (v_q, 1, 'Estoy', true);

  INSERT INTO homework_questions (assignment_id, position, kind, prompt)
  VALUES (v_hw2, 1, 'SINGLE_CHOICE', '2. Este café (es / está) muy caliente.')
  RETURNING id INTO v_q;
  INSERT INTO homework_question_options (question_id, position, label, is_correct) VALUES
    (v_q, 0, 'es', false),
    (v_q, 1, 'está', true);

  INSERT INTO homework_questions (assignment_id, position, kind, prompt)
  VALUES (v_hw2, 2, 'SINGLE_CHOICE', '3. Algunas rosas (son / están) blancas.')
  RETURNING id INTO v_q;
  INSERT INTO homework_question_options (question_id, position, label, is_correct) VALUES
    (v_q, 0, 'son', true),
    (v_q, 1, 'están', false);

  INSERT INTO homework_questions (assignment_id, position, kind, prompt)
  VALUES (v_hw2, 3, 'SINGLE_CHOICE', '4. Esos cristales (son / están) sucios.')
  RETURNING id INTO v_q;
  INSERT INTO homework_question_options (question_id, position, label, is_correct) VALUES
    (v_q, 0, 'son', false),
    (v_q, 1, 'están', true);

  INSERT INTO homework_questions (assignment_id, position, kind, prompt)
  VALUES (v_hw2, 4, 'SINGLE_CHOICE', '5. Este árbol (es / está) muerto.')
  RETURNING id INTO v_q;
  INSERT INTO homework_question_options (question_id, position, label, is_correct) VALUES
    (v_q, 0, 'es', false),
    (v_q, 1, 'está', true);

  INSERT INTO homework_questions (assignment_id, position, kind, prompt)
  VALUES (v_hw2, 5, 'SINGLE_CHOICE', '6. Ana y Sergio (son / están) casados.')
  RETURNING id INTO v_q;
  INSERT INTO homework_question_options (question_id, position, label, is_correct) VALUES
    (v_q, 0, 'son', false),
    (v_q, 1, 'están', true);

  INSERT INTO homework_questions (assignment_id, position, kind, prompt)
  VALUES (v_hw2, 6, 'SINGLE_CHOICE', '7. Las margaritas (son / están) amarillas y blancas.')
  RETURNING id INTO v_q;
  INSERT INTO homework_question_options (question_id, position, label, is_correct) VALUES
    (v_q, 0, 'son', true),
    (v_q, 1, 'están', false);

  INSERT INTO homework_questions (assignment_id, position, kind, prompt)
  VALUES (v_hw2, 7, 'SINGLE_CHOICE', '8. ¡Qué guapa (eres / estás) con ese sombrero, Lola!')
  RETURNING id INTO v_q;
  INSERT INTO homework_question_options (question_id, position, label, is_correct) VALUES
    (v_q, 0, 'eres', false),
    (v_q, 1, 'estás', true);

  INSERT INTO homework_questions (assignment_id, position, kind, prompt)
  VALUES (v_hw2, 8, 'SINGLE_CHOICE', '9. Hoy no (soy / estoy) alegre.')
  RETURNING id INTO v_q;
  INSERT INTO homework_question_options (question_id, position, label, is_correct) VALUES
    (v_q, 0, 'soy', false),
    (v_q, 1, 'estoy', true);

  -- 4.2 Complete las frases (sentence 1 is the worksheet example)
  INSERT INTO homework_questions (assignment_id, position, kind, prompt, structure_json)
  VALUES (
      v_hw2, 9, 'MULTI_BLANK',
      $p$1. Las serpientes ___ peligrosas.
2. Este libro ___ muy interesante.
3. La nieve ___ blanca.
4. No os sentéis en esas sillas. ___ sucias.
5. Juan ___ muy guapo con uniforme.
6. La sopa ___ fría. Caliéntala un poco.
7. Los plátanos ___ amarillos.
8. ¡Qué guapo ___ Francisco! Tiene unos ojos grandísimos.
9. Flor y Pili ___ muy guapas con el nuevo peinado.$p$,
      $j${
        "blanks": [
          {"acceptedAnswers": ["son"]},
          {"acceptedAnswers": ["es"]},
          {"acceptedAnswers": ["es"]},
          {"acceptedAnswers": ["Están", "están", "Estan", "estan"]},
          {"acceptedAnswers": ["está", "esta"]},
          {"acceptedAnswers": ["está", "esta"]},
          {"acceptedAnswers": ["son", "están", "estan"]},
          {"acceptedAnswers": ["es"]},
          {"acceptedAnswers": ["están", "estan"]}
        ]
      }$j$::jsonb
  );

  -- =========================================================================
  -- Homework 3 — fill in the gaps (Carlos & Ana)
  -- =========================================================================
  INSERT INTO homework_assignments (
      title, instructions, published, format, homework_type, level, sort_order
  ) VALUES (
      'C3 · Diálogo: la casa de verano (ser / estar)',
      'Completa el diálogo con la forma correcta de ser o estar.',
      true, 'EXERCISE', 'GRAMMAR', 'A1', 0
  ) RETURNING id INTO v_hw3;

  INSERT INTO homework_questions (assignment_id, position, kind, prompt, structure_json)
  VALUES (
      v_hw3, 0, 'MULTI_BLANK',
      $p$Carlos: Mira, Ana: esta foto de mi Instagram es de nuestra casa de verano.
Ana: ¿Sí? ___ muy bonita. ¿Dónde ___?
Carlos: La casa ___ en Torremolinos. Torremolinos ___ un pueblo de Málaga y ___ en el sur del país. ¡Me encanta España!
Ana: ¡Qué guay!
Ana: ¡Ah! ¡Qué bonitos! ¿Y dónde ___ tu dormitorio?
Carlos: Mi dormitorio ___ en la segunda planta, entre el baño y la habitación de mis padres. No ___ muy grande pero me gusta mucho.
Ana: También tenéis jardín, ¿no?
Carlos: Sí, el jardín ___ detrás de la casa y allí también ___ la piscina.$p$,
      $j${
        "blanks": [
          {"acceptedAnswers": ["Es", "es"]},
          {"acceptedAnswers": ["está", "esta"]},
          {"acceptedAnswers": ["está", "esta"]},
          {"acceptedAnswers": ["es"]},
          {"acceptedAnswers": ["está", "esta"]},
          {"acceptedAnswers": ["está", "esta"]},
          {"acceptedAnswers": ["está", "esta"]},
          {"acceptedAnswers": ["es"]},
          {"acceptedAnswers": ["está", "esta"]},
          {"acceptedAnswers": ["está", "esta"]}
        ]
      }$j$::jsonb
  );

  -- =========================================================================
  -- Homework 4 — select the correct option (A2 worksheet)
  -- One SINGLE_CHOICE per ser/estar pair.
  -- =========================================================================
  INSERT INTO homework_assignments (
      title, instructions, published, format, homework_type, level, sort_order
  ) VALUES (
      'C3 · ¿Ser o estar? Esa es la cuestión',
      'Elige la opción correcta. Fíjate en el contexto y en el adjetivo: no es lo mismo ser bueno que estar bueno, ni ser abierto que estar abierto.',
      true, 'EXERCISE', 'GRAMMAR', 'A2', 0
  ) RETURNING id INTO v_hw4;

  -- Ramón
  INSERT INTO homework_questions (assignment_id, position, kind, prompt)
  VALUES (v_hw4, 0, 'SINGLE_CHOICE',
          'Ramón: «Normalmente (soy / estoy) muy alegre, pero hoy estoy/soy muy triste.» — primer verbo')
  RETURNING id INTO v_q;
  INSERT INTO homework_question_options (question_id, position, label, is_correct) VALUES
    (v_q, 0, 'soy', true),
    (v_q, 1, 'estoy', false);

  INSERT INTO homework_questions (assignment_id, position, kind, prompt)
  VALUES (v_hw4, 1, 'SINGLE_CHOICE',
          'Ramón: «Normalmente soy/estoy muy alegre, pero hoy (soy / estoy) muy triste.» — segundo verbo')
  RETURNING id INTO v_q;
  INSERT INTO homework_question_options (question_id, position, label, is_correct) VALUES
    (v_q, 0, 'soy', false),
    (v_q, 1, 'estoy', true);

  -- Carlitos
  INSERT INTO homework_questions (assignment_id, position, kind, prompt)
  VALUES (v_hw4, 2, 'SINGLE_CHOICE',
          'Carlitos: «Creo que (soy / estoy) bastante listo, pero todavía no soy/estoy listo para entrar en la uni.» — primer verbo')
  RETURNING id INTO v_q;
  INSERT INTO homework_question_options (question_id, position, label, is_correct) VALUES
    (v_q, 0, 'soy', true),
    (v_q, 1, 'estoy', false);

  INSERT INTO homework_questions (assignment_id, position, kind, prompt)
  VALUES (v_hw4, 3, 'SINGLE_CHOICE',
          'Carlitos: «Creo que soy/estoy bastante listo, pero todavía no (soy / estoy) listo para entrar en la uni.» — segundo verbo')
  RETURNING id INTO v_q;
  INSERT INTO homework_question_options (question_id, position, label, is_correct) VALUES
    (v_q, 0, 'soy', false),
    (v_q, 1, 'estoy', true);

  -- Isa
  INSERT INTO homework_questions (assignment_id, position, kind, prompt)
  VALUES (v_hw4, 4, 'SINGLE_CHOICE',
          'Isa: «(Soy / Estoy) siempre muy contenta, no soy/estoy una persona triste.» — primer verbo')
  RETURNING id INTO v_q;
  INSERT INTO homework_question_options (question_id, position, label, is_correct) VALUES
    (v_q, 0, 'Soy', true),
    (v_q, 1, 'Estoy', false);

  INSERT INTO homework_questions (assignment_id, position, kind, prompt)
  VALUES (v_hw4, 5, 'SINGLE_CHOICE',
          'Isa: «Soy/Estoy siempre muy contenta, no (soy / estoy) una persona triste.» — segundo verbo')
  RETURNING id INTO v_q;
  INSERT INTO homework_question_options (question_id, position, label, is_correct) VALUES
    (v_q, 0, 'soy', true),
    (v_q, 1, 'estoy', false);

  -- Rafa
  INSERT INTO homework_questions (assignment_id, position, kind, prompt)
  VALUES (v_hw4, 6, 'SINGLE_CHOICE',
          'Rafa: «(Estoy / Soy) malito desde hace dos días y no soy/estoy muy católico.» — primer verbo')
  RETURNING id INTO v_q;
  INSERT INTO homework_question_options (question_id, position, label, is_correct) VALUES
    (v_q, 0, 'Estoy', true),
    (v_q, 1, 'Soy', false);

  INSERT INTO homework_questions (assignment_id, position, kind, prompt)
  VALUES (v_hw4, 7, 'SINGLE_CHOICE',
          'Rafa: «Estoy/Soy malito desde hace dos días y no (soy / estoy) muy católico.» — segundo verbo')
  RETURNING id INTO v_q;
  INSERT INTO homework_question_options (question_id, position, label, is_correct) VALUES
    (v_q, 0, 'soy', false),
    (v_q, 1, 'estoy', true);

  -- Tere (one pair)
  INSERT INTO homework_questions (assignment_id, position, kind, prompt)
  VALUES (v_hw4, 8, 'SINGLE_CHOICE',
          'Tere: «Me parece que no sé muchas cosas de la vida, (soy / estoy) muy verde todavía.»')
  RETURNING id INTO v_q;
  INSERT INTO homework_question_options (question_id, position, label, is_correct) VALUES
    (v_q, 0, 'soy', false),
    (v_q, 1, 'estoy', true);

  -- Santi
  INSERT INTO homework_questions (assignment_id, position, kind, prompt)
  VALUES (v_hw4, 9, 'SINGLE_CHOICE',
          'Santi: «Creo que (soy / estoy) muy bueno porque soy/estoy rico desde hace un año y ayudo a los pobres.» — primer verbo')
  RETURNING id INTO v_q;
  INSERT INTO homework_question_options (question_id, position, label, is_correct) VALUES
    (v_q, 0, 'soy', true),
    (v_q, 1, 'estoy', false);

  INSERT INTO homework_questions (assignment_id, position, kind, prompt)
  VALUES (v_hw4, 10, 'SINGLE_CHOICE',
          'Santi: «Creo que soy/estoy muy bueno porque (soy / estoy) rico desde hace un año y ayudo a los pobres.» — segundo verbo')
  RETURNING id INTO v_q;
  INSERT INTO homework_question_options (question_id, position, label, is_correct) VALUES
    (v_q, 0, 'soy', true),
    (v_q, 1, 'estoy', false);

  -- Mercedes
  INSERT INTO homework_questions (assignment_id, position, kind, prompt)
  VALUES (v_hw4, 11, 'SINGLE_CHOICE',
          'Mercedes: «Dicen que la calidad del mazapán de Toledo (es / está) buena pero lo he probado y es/está muy malo.» — primer verbo')
  RETURNING id INTO v_q;
  INSERT INTO homework_question_options (question_id, position, label, is_correct) VALUES
    (v_q, 0, 'es', true),
    (v_q, 1, 'está', false);

  INSERT INTO homework_questions (assignment_id, position, kind, prompt)
  VALUES (v_hw4, 12, 'SINGLE_CHOICE',
          'Mercedes: «Dicen que la calidad del mazapán de Toledo es/está buena pero lo he probado y (es / está) muy malo.» — segundo verbo')
  RETURNING id INTO v_q;
  INSERT INTO homework_question_options (question_id, position, label, is_correct) VALUES
    (v_q, 0, 'es', false),
    (v_q, 1, 'está', true);

  -- Andrea
  INSERT INTO homework_questions (assignment_id, position, kind, prompt)
  VALUES (v_hw4, 13, 'SINGLE_CHOICE',
          'Andrea: «Me gusta mucho mi nuevo compi de piso, (es / está) muy abierto y además es/está buenísimo.» — primer verbo')
  RETURNING id INTO v_q;
  INSERT INTO homework_question_options (question_id, position, label, is_correct) VALUES
    (v_q, 0, 'es', true),
    (v_q, 1, 'está', false);

  INSERT INTO homework_questions (assignment_id, position, kind, prompt)
  VALUES (v_hw4, 14, 'SINGLE_CHOICE',
          'Andrea: «Me gusta mucho mi nuevo compi de piso, es/está muy abierto y además (es / está) buenísimo.» — segundo verbo')
  RETURNING id INTO v_q;
  INSERT INTO homework_question_options (question_id, position, label, is_correct) VALUES
    (v_q, 0, 'es', false),
    (v_q, 1, 'está', true);

  -- Ernesto
  INSERT INTO homework_questions (assignment_id, position, kind, prompt)
  VALUES (v_hw4, 15, 'SINGLE_CHOICE',
          'Ernesto: «Ahora (soy / estoy) enfadado, pero en general soy/estoy muy alegre.» — primer verbo')
  RETURNING id INTO v_q;
  INSERT INTO homework_question_options (question_id, position, label, is_correct) VALUES
    (v_q, 0, 'soy', false),
    (v_q, 1, 'estoy', true);

  INSERT INTO homework_questions (assignment_id, position, kind, prompt)
  VALUES (v_hw4, 16, 'SINGLE_CHOICE',
          'Ernesto: «Ahora soy/estoy enfadado, pero en general (soy / estoy) muy alegre.» — segundo verbo')
  RETURNING id INTO v_q;
  INSERT INTO homework_question_options (question_id, position, label, is_correct) VALUES
    (v_q, 0, 'soy', true),
    (v_q, 1, 'estoy', false);

  -- Jorge (three pairs)
  INSERT INTO homework_questions (assignment_id, position, kind, prompt)
  VALUES (v_hw4, 17, 'SINGLE_CHOICE',
          'Jorge: «Mi mejor amigo (es / está) católico, es/está muy despierto y siempre es/está muy atento conmigo.» — primer verbo')
  RETURNING id INTO v_q;
  INSERT INTO homework_question_options (question_id, position, label, is_correct) VALUES
    (v_q, 0, 'es', true),
    (v_q, 1, 'está', false);

  INSERT INTO homework_questions (assignment_id, position, kind, prompt)
  VALUES (v_hw4, 18, 'SINGLE_CHOICE',
          'Jorge: «Mi mejor amigo es/está católico, (es / está) muy despierto y siempre es/está muy atento conmigo.» — segundo verbo')
  RETURNING id INTO v_q;
  INSERT INTO homework_question_options (question_id, position, label, is_correct) VALUES
    (v_q, 0, 'es', true),
    (v_q, 1, 'está', false);

  INSERT INTO homework_questions (assignment_id, position, kind, prompt)
  VALUES (v_hw4, 19, 'SINGLE_CHOICE',
          'Jorge: «Mi mejor amigo es/está católico, es/está muy despierto y siempre (es / está) muy atento conmigo.» — tercer verbo')
  RETURNING id INTO v_q;
  INSERT INTO homework_question_options (question_id, position, label, is_correct) VALUES
    (v_q, 0, 'es', true),
    (v_q, 1, 'está', false);

  RAISE NOTICE 'Created homeworks: % / % / % / %', v_hw1, v_hw2, v_hw3, v_hw4;
END
$body$;

COMMIT;

-- Verify
SELECT a.id, a.title, a.format, a.homework_type, a.level, a.unit_id,
       (SELECT COUNT(*) FROM homework_questions q WHERE q.assignment_id = a.id) AS questions
FROM homework_assignments a
WHERE a.title LIKE 'C3 · %'
ORDER BY a.created_at;
