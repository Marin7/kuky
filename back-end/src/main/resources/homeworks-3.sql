-- Reading homework from "DELE_C1_0424_P1_P2_COD17.pdf" (pages 2–3)
-- DELE C1 · Prueba 1 (comprensión de lectura y uso de la lengua), Tarea 1
-- Contrato de alquiler de coches + 6 SINGLE_CHOICE questions (1–6).
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
  -- Comprensión de lectura — contrato de alquiler de coches
  -- =========================================================================
  INSERT INTO homework_assignments (
      title, instructions, published, format, homework_type, level, sort_order
  ) VALUES (
      'DELE C1 · Comprensión de lectura: contrato de alquiler de coches',
      $i$Lee el siguiente contrato de arrendamiento de coches. Luego, responde las preguntas (1-6) seleccionando la opción correcta (a, b o c).

Contrato de alquiler de coches

1. Objeto
El objeto del presente contrato es el arrendamiento del vehículo elegido por el arrendatario y descrito en las condiciones particulares que la empresa pone a su disposición.

2. Precio
Los cargos de alquiler y de servicios de mantenimiento indicados en las condiciones particulares permanecerán fijos durante la duración del contrato. El retraso de los pagos derivados del presente contrato dará lugar a intereses de demora.
El hecho de que el vehículo no pueda ser utilizado por cualquier razón no imputable a la empresa arrendadora no podrá ser alegado por el arrendatario como motivo para no cumplir sus obligaciones de pago.

3. Propiedad
La firma de este contrato no atribuye al arrendatario ningún derecho de propiedad sobre el vehículo arrendado, de manera que no podrá cederlo temporalmente ni subarrendarlo.
El arrendatario se obliga a notificar a la empresa con la máxima urgencia cualquier incidente sobre el vehículo arrendado provocado por terceros.

4. Utilización
El arrendatario se obliga a utilizar con diligencia y cuidado el vehículo conforme al uso a que está destinado, debiendo realizar las revisiones que se indican en los manuales de mantenimiento.
Queda prohibida, salvo autorización expresa, su utilización para el transporte regular de terceras personas y, bajo ningún concepto, estará permitido el transporte remunerado de viajeros o mercancías, así como el uso para actividades de autoescuela. Queda igualmente prohibida su utilización en competiciones deportivas o pruebas de cualquier clase, así como enganchar remolques o efectuar en el vehículo, o en cualesquiera de sus piezas o accesorios, alguna modificación. El vehículo tampoco podrá salir del territorio español sin autorización previa y por escrito de la compañía.
El arrendatario se obliga a llevar en el vehículo la documentación pertinente, legalmente exigible para su circulación, y que le ha sido entregada al realizar el alquiler.

5. Mantenimiento y reparación
La empresa arrendadora será la responsable de gestionar, tramitar y cubrir, por sí o por medio de terceros que ella designe, todos los gastos derivados del mantenimiento y reparación del vehículo arrendado.
El arrendatario será responsable de cumplir las normas siguientes:
– Todas las reparaciones mecánicas y mantenimiento del vehículo serán realizados en concesionarios oficiales de la marca del vehículo arrendado, o en los talleres designados por la empresa arrendadora.
– No se podrá ordenar la ejecución de trabajos de reparación o revisiones del vehículo sin ponerlo en conocimiento de la compañía, que deberá autorizar o denegar la solicitud en un plazo de 72 horas. De no hacerlo así, se entenderá a todos los efectos autorizada.
No estará incluido en el programa de mantenimiento que costea la empresa el lavado, encerado y limpieza interior del vehículo, la reposición de moquetas y alfombras de suelo, el mantenimiento y reparación de navegadores (y sus actualizaciones) y de equipos de música y telefónico, aunque estos vinieran de serie en el vehículo. Sí queda, sin embargo, incluida la sustitución de cada uno de los neumáticos que estén en uso, de acuerdo con lo establecido en las condiciones particulares. Los nuevos neumáticos deberán ser de características, calidades y prestaciones nunca inferiores a los originales.

6. Seguro
La empresa contratará un seguro a todo riesgo en el que ella figurará como beneficiaria y el arrendatario como asegurado, siendo este quien tendrá que asumir el importe. La empresa se reserva el derecho de determinar la compañía aseguradora en cada momento a lo largo del contrato, de todo lo cual informará al arrendatario.

7. Devolución
Al finalizar el plazo contractual pactado, el arrendatario se obliga a devolver a la empresa el vehículo junto con toda su documentación y accesorios, en correcto estado de funcionamiento y en las condiciones derivadas del buen uso durante su tiempo de utilización.
Una vez efectuada la entrega, las partes verificarán el estado del coche y el de sus elementos, firmando el correspondiente documento de devolución, en el que se harán constar las deficiencias o anomalías que se hubieran detectado y el kilometraje final del vehículo. En caso de disconformidad, las partes, actuando por medio de sus representantes autorizados, efectuarán un examen minucioso, con el fin de determinar el estado real y las posibles responsabilidades.
Si por motivos no imputables a la empresa el arrendatario no procediera a la devolución del vehículo, estará obligado a pagar, en concepto de indemnización por la continuación de su uso y disfrute, una cantidad por cada día de retraso y, además, la compañía podrá proceder a su retirada allí donde se encuentre, para lo que queda irrevocable y expresamente autorizada.$i$,
      true, 'EXERCISE', 'READ', 'C1', 0
  ) RETURNING id INTO v_hw;

  -- 1 --------------------------------------------------------------------
  INSERT INTO homework_questions (assignment_id, position, kind, prompt)
  VALUES (v_hw, 0, 'SINGLE_CHOICE', 'En el contrato se indica que…')
  RETURNING id INTO v_q;
  INSERT INTO homework_question_options (question_id, position, label, is_correct) VALUES
      (v_q, 0, 'las condiciones están sujetas a posibles imprevistos.', false),
      (v_q, 1, 'existe un recargo económico por retrasos en el pago.', true),
      (v_q, 2, 'la empresa queda al margen de los daños a terceros.', false);

  -- 2 --------------------------------------------------------------------
  INSERT INTO homework_questions (assignment_id, position, kind, prompt)
  VALUES (v_hw, 1, 'SINGLE_CHOICE', 'Según el texto, se requerirá un permiso para…')
  RETURNING id INTO v_q;
  INSERT INTO homework_question_options (question_id, position, label, is_correct) VALUES
      (v_q, 0, 'dedicar el vehículo a un uso de carácter comercial.', false),
      (v_q, 1, 'sobrepasar con el automóvil los límites nacionales.', true),
      (v_q, 2, 'sustituir alguno de los elementos propios del coche.', false);

  -- 3 --------------------------------------------------------------------
  INSERT INTO homework_questions (assignment_id, position, kind, prompt)
  VALUES (v_hw, 2, 'SINGLE_CHOICE',
          'En cuanto a las reparaciones, el texto señala que el arrendatario…')
  RETURNING id INTO v_q;
  INSERT INTO homework_question_options (question_id, position, label, is_correct) VALUES
      (v_q, 0, 'tiene que asumir el pago de las facturas.', false),
      (v_q, 1, 'puede elegir un taller que ofrezca garantías.', false),
      (v_q, 2, 'debe notificarlas a la compañía con antelación.', true);

  -- 4 --------------------------------------------------------------------
  INSERT INTO homework_questions (assignment_id, position, kind, prompt)
  VALUES (v_hw, 3, 'SINGLE_CHOICE',
          'Según el texto, el programa de mantenimiento obliga a la empresa a…')
  RETURNING id INTO v_q;
  INSERT INTO homework_question_options (question_id, position, label, is_correct) VALUES
      (v_q, 0, 'reemplazar las ruedas cuando se precise.', true),
      (v_q, 1, 'mantener la correcta higiene del vehículo.', false),
      (v_q, 2, 'revisar todos los dispositivos tecnológicos.', false);

  -- 5 --------------------------------------------------------------------
  INSERT INTO homework_questions (assignment_id, position, kind, prompt)
  VALUES (v_hw, 4, 'SINGLE_CHOICE', 'Sobre el seguro del vehículo, se explica que…')
  RETURNING id INTO v_q;
  INSERT INTO homework_question_options (question_id, position, label, is_correct) VALUES
      (v_q, 0, 'la elección de compañía es responsabilidad de la empresa.', true),
      (v_q, 1, 'el arrendatario pagará una parte proporcional de su coste.', false),
      (v_q, 2, 'se determinarán las características por común acuerdo.', false);

  -- 6 --------------------------------------------------------------------
  INSERT INTO homework_questions (assignment_id, position, kind, prompt)
  VALUES (v_hw, 5, 'SINGLE_CHOICE',
          'Respecto a la devolución del vehículo, el texto señala que…')
  RETURNING id INTO v_q;
  INSERT INTO homework_question_options (question_id, position, label, is_correct) VALUES
      (v_q, 0, 'se cobrará una penalización en caso de observarse cualquier deterioro.', false),
      (v_q, 1, 'se hará una revisión exhaustiva del coche si existe algún tipo de desacuerdo.', true),
      (v_q, 2, 'se procederá a su retirada, por parte de la compañía, en un lugar específico.', false);

  RAISE NOTICE 'Created homework: %', v_hw;
END
$body$;

COMMIT;

-- Verify
SELECT a.id, a.title, a.format, a.homework_type, a.level, a.unit_id,
       (SELECT COUNT(*) FROM homework_questions q WHERE q.assignment_id = a.id) AS questions
FROM homework_assignments a
WHERE a.title = 'DELE C1 · Comprensión de lectura: contrato de alquiler de coches'
ORDER BY a.created_at;
