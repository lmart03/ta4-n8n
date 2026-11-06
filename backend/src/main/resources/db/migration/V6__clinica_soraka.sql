-- El caso del informe es la Clínica Soraka (ficticia, Ica). Se reemplaza el nombre provisional "Consultorio Demo Ica".
-- Horarios, dirección y teléfono siguen siendo datos de ejemplo: el informe no entrega valores reales y no se inventan.
UPDATE business_info SET
  business_name = 'Clínica Soraka',
  description = 'Clínica privada ficticia de Ica, usada exclusivamente para una demostración académica. Ofrece medicina general, pediatría, ginecología, dermatología y laboratorio clínico.',
  appointment_instructions = 'Para solicitar una cita, comunícate con recepción por el canal de contacto que configure el equipo; esta conversación no registra ni confirma reservas.'
WHERE id = 'f8c577a4-353d-486e-91d0-7504ddb836b0';

UPDATE faq SET answer = 'Dato ficticio de ejemplo: los domingos la clínica permanece cerrada.' WHERE id = '00000000-0000-4000-8000-000000000003';
UPDATE faq SET answer = 'Dato ficticio de ejemplo: estamos en Av. Los Ejemplos 123, distrito y ciudad de Ica.' WHERE id = '00000000-0000-4000-8000-000000000004';
UPDATE faq SET
  answer = 'La Clínica Soraka ofrece medicina general, pediatría, ginecología, dermatología y laboratorio clínico. Para otros servicios, consulta a recepción.',
  keywords = 'servicios ofrecen especialidades especialidad consulta atencion medicina general pediatria ginecologia dermatologia laboratorio'
WHERE id = '00000000-0000-4000-8000-000000000005';
UPDATE faq SET answer = 'El precio de la consulta aún no está configurado. Solicita la tarifa actual a recepción por el canal de contacto de la clínica.' WHERE id = '00000000-0000-4000-8000-000000000006';
UPDATE faq SET answer = 'No dispongo de una tarifa para ese servicio. Consulta el precio directamente con recepción.' WHERE id = '00000000-0000-4000-8000-000000000007';
UPDATE faq SET
  question = '¿Cómo me comunico con la clínica?',
  answer = 'El teléfono de contacto aún no está configurado para esta demostración. Consulta a recepción por el canal que te proporcionó este bot.'
WHERE id = '00000000-0000-4000-8000-000000000008';
UPDATE faq SET answer = 'Puedes solicitar una cita comunicándote con recepción por el canal de contacto que configure el equipo. Este bot no registra ni confirma reservas.' WHERE id = '00000000-0000-4000-8000-000000000009';
UPDATE faq SET answer = 'No puedo reservar ni confirmar una cita. Para consultar disponibilidad y solicitarla, comunícate con recepción.' WHERE id = '00000000-0000-4000-8000-000000000010';
UPDATE faq SET answer = 'No dispongo de información sobre atención de urgencias en la Clínica Soraka. Ante una urgencia real, acude a un servicio de emergencia local.' WHERE id = '00000000-0000-4000-8000-000000000011';
UPDATE faq SET answer = 'Dato ficticio de ejemplo: la Clínica Soraka está en Ica, Perú.' WHERE id = '00000000-0000-4000-8000-000000000012';

INSERT INTO faq (id,category,question,answer,keywords,active,sort_order) VALUES
('00000000-0000-4000-8000-000000000013','SERVICIOS','¿Tienen laboratorio clínico?','Sí, la Clínica Soraka cuenta con laboratorio clínico. No dispongo de la lista de análisis ni de sus precios; consúltalos en recepción.','laboratorio clinico analisis examenes examen sangre orina',true,13),
('00000000-0000-4000-8000-000000000014','SERVICIOS','¿Atienden niños?','Sí, la clínica ofrece pediatría. Para conocer la disponibilidad, comunícate con recepción.','pediatria pediatra ninos nino bebe hijo atienden',true,14),
('00000000-0000-4000-8000-000000000015','SERVICIOS','¿Tienen ginecología o dermatología?','Sí, la clínica ofrece ginecología y dermatología. No dispongo de horarios ni tarifas por especialidad; consúltalos en recepción.','ginecologia ginecologo dermatologia dermatologo piel especialidades',true,15);
