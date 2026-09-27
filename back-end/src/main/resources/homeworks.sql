-- C7 · Escucha: Juanes – A Dios le pido — completa la letra con el verbo
-- entre paréntesis en presente de subjuntivo.
-- One MULTI_BLANK question covering the whole song passage.
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

  INSERT INTO homework_assignments (
      title, instructions, published, format, homework_type, level, sort_order,
      media_source_kind, audio_url
  ) VALUES (
      'C7 · Escucha: Juanes – A Dios le pido',
      'Escucha la canción "A Dios le pido" de Juanes, presta atención al estribillo y completa la letra conjugando cada verbo entre paréntesis en presente de subjuntivo.',
      true, 'EXERCISE', 'AUDIO', 'B1', 0,
      'YOUTUBE', 'https://www.youtube.com/watch?v=kMIaYXxLnUA'
  ) RETURNING id INTO v_hw;

  INSERT INTO homework_questions (assignment_id, position, kind, prompt, structure_json)
  VALUES
  (v_hw, 0, 'MULTI_BLANK',
   $p$Que mis ojos ___ (despertarse) con la luz de tu mirada, yo
a Dios le pido.
Que mi madre no ___ (morirse) y que mi padre ___ (recordarme),
a Dios le pido.
Que ___ (quedarte) a mi lado y que más nunca te me ___ (ir), mi vida,
a Dios le pido.
Que mi alma no ___ (descansar) cuando de amarte se trate, mi cielo,
a Dios le pido.

Por los días que me quedan y las noches que aún no llegan yo,
a Dios le pido.
Por los hijos de mis hijos y los hijos de tus hijos,
a Dios le pido.
Que mi pueblo no ___ (derramar) tanta sangre y ___ (levantarse) mi gente,
a Dios le pido.
Que mi alma no ___ (descansar) cuando de amarte se trate, mi cielo,
a Dios le pido.

Un segundo más de vida para darte y mi corazón
entero entregarte.
Un segundo más de vida para darte y a tu lado para
siempre yo quedarme.
Un segundo más de vida yo...
a Dios le pido.

Que si me muero ___ (ser) de amor.
Y si me enamoro ___ (ser) de vos.
Y que de tu voz ___ (ser) este corazón todos los días,
a Dios le pido.

Que si me muero ___ (ser) de amor.
y si me enamoro ___ (ser) de vos.
Y que de tu voz ___ (ser) este corazón
Todos los días a Dios le pido
a Dios le pido.$p$,
   $j${
     "blanks": [
       {"acceptedAnswers": ["se despierten"]},
       {"acceptedAnswers": ["se muera"]},
       {"acceptedAnswers": ["me recuerde"]},
       {"acceptedAnswers": ["te quedes"]},
       {"acceptedAnswers": ["vayas"]},
       {"acceptedAnswers": ["descanse"]},
       {"acceptedAnswers": ["derrame"]},
       {"acceptedAnswers": ["se levante"]},
       {"acceptedAnswers": ["descanse"]},
       {"acceptedAnswers": ["sea"]},
       {"acceptedAnswers": ["sea"]},
       {"acceptedAnswers": ["sea"]},
       {"acceptedAnswers": ["sea"]},
       {"acceptedAnswers": ["sea"]},
       {"acceptedAnswers": ["sea"]}
     ]
   }$j$::jsonb);

  RAISE NOTICE 'Created homework: %', v_hw;
END
$body$;

COMMIT;

-- Verify
SELECT a.id, a.title, a.format, a.homework_type, a.level, a.unit_id,
       a.media_source_kind, a.audio_url,
       (SELECT COUNT(*) FROM homework_questions q WHERE q.assignment_id = a.id) AS questions
FROM homework_assignments a
WHERE a.title = 'C7 · Escucha: Juanes – A Dios le pido'
ORDER BY a.created_at;
