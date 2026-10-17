-- Adjetivos: concordancia de género y número — completa las frases con la
-- forma correcta del adjetivo entre paréntesis.
-- One MULTI_BLANK question per numbered item (11 questions).
-- Unassigned: no homework_targets. Assign later from /panel.
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
      title, instructions, published, format, homework_type, level, sort_order
  ) VALUES (
      'Adjetivos: género y número',
      'Completa las frases con la forma correcta de los adjetivos entre paréntesis.',
      true, 'EXERCISE', 'GRAMMAR', 'A1', 0
  ) RETURNING id INTO v_hw;

  INSERT INTO homework_questions (assignment_id, position, kind, prompt, structure_json)
  VALUES
  (v_hw, 0, 'MULTI_BLANK',
   $p$Vivo en un piso ___ (pequeño).$p$,
   $j${"blanks": [
     {"acceptedAnswers": ["pequeño"]}
   ]}$j$::jsonb),

  (v_hw, 1, 'MULTI_BLANK',
   $p$Rosa tiene los ojos ___ (verde). Es ___ (rubio) y tiene el pelo ___ (corto) y ___ (rizado). Es muy ___ (simpático).$p$,
   $j${"blanks": [
     {"acceptedAnswers": ["verdes"]},
     {"acceptedAnswers": ["rubia"]},
     {"acceptedAnswers": ["corto"]},
     {"acceptedAnswers": ["rizado"]},
     {"acceptedAnswers": ["simpática"]}
   ]}$j$::jsonb),

  (v_hw, 2, 'MULTI_BLANK',
   $p$Gerardo y Ana son muy ___ (inteligente), ___ (alegre) y muy ___ (charlatán).$p$,
   $j${"blanks": [
     {"acceptedAnswers": ["inteligentes"]},
     {"acceptedAnswers": ["alegres"]},
     {"acceptedAnswers": ["charlatanes"]}
   ]}$j$::jsonb),

  (v_hw, 3, 'MULTI_BLANK',
   $p$Me gustan las rosas ___ (blanco).$p$,
   $j${"blanks": [
     {"acceptedAnswers": ["blancas"]}
   ]}$j$::jsonb),

  (v_hw, 4, 'MULTI_BLANK',
   $p$Don Santiago es una persona ___ (encantador).$p$,
   $j${"blanks": [
     {"acceptedAnswers": ["encantadora"]}
   ]}$j$::jsonb),

  (v_hw, 5, 'MULTI_BLANK',
   $p$Chus y Lolo no son ___ (feliz). Están siempre ___ (enfadado).$p$,
   $j${"blanks": [
     {"acceptedAnswers": ["felices"]},
     {"acceptedAnswers": ["enfadados"]}
   ]}$j$::jsonb),

  (v_hw, 6, 'MULTI_BLANK',
   $p$Me he comprado una camisa ___ (rojo) y unos pantalones ___ (gris).$p$,
   $j${"blanks": [
     {"acceptedAnswers": ["roja"]},
     {"acceptedAnswers": ["grises"]}
   ]}$j$::jsonb),

  (v_hw, 7, 'MULTI_BLANK',
   $p$Eduardo es ___ (alto) y ___ (fuerte). Es ___ (moreno) y tiene los ojos ___ (negro). Es muy ___ (guapo), pero es un poco ___ (antipático).$p$,
   $j${"blanks": [
     {"acceptedAnswers": ["alto"]},
     {"acceptedAnswers": ["fuerte"]},
     {"acceptedAnswers": ["moreno"]},
     {"acceptedAnswers": ["negros"]},
     {"acceptedAnswers": ["guapo"]},
     {"acceptedAnswers": ["antipático"]}
   ]}$j$::jsonb),

  (v_hw, 8, 'MULTI_BLANK',
   $p$Begoña lleva una falda ___ (marrón) y una blusa ___ (amarillo).$p$,
   $j${"blanks": [
     {"acceptedAnswers": ["marrón"]},
     {"acceptedAnswers": ["amarilla"]}
   ]}$j$::jsonb),

  (v_hw, 9, 'MULTI_BLANK',
   $p$Concha y Rodrigo son muy ___ (simpático) y muy ___ (trabajador). Además son muy ___ (amable).$p$,
   $j${"blanks": [
     {"acceptedAnswers": ["simpáticos"]},
     {"acceptedAnswers": ["trabajadores"]},
     {"acceptedAnswers": ["amables"]}
   ]}$j$::jsonb),

  (v_hw, 10, 'MULTI_BLANK',
   $p$Las hijas de Andrés son muy ___ (joven).$p$,
   $j${"blanks": [
     {"acceptedAnswers": ["jóvenes"]}
   ]}$j$::jsonb);

  RAISE NOTICE 'Created homework: %', v_hw;
END
$body$;

COMMIT;

-- Verify
SELECT a.id, a.title, a.format, a.homework_type, a.level,
       (SELECT COUNT(*) FROM homework_questions q WHERE q.assignment_id = a.id) AS questions
FROM homework_assignments a
WHERE a.title = 'Adjetivos: género y número'
ORDER BY a.created_at;
