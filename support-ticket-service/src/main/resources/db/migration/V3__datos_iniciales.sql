-- Initial support team. These technicians have no login account yet: the application creates the
-- accounts at startup when a bootstrap password is configured (see README), or an admin creates new
-- technicians through POST /technicians. Codes TEC-001..TEC-015 match technician-dispatch-service.

INSERT INTO technicians (code, user_id, full_name, specialty, site, max_active_tickets, status) VALUES
  ('TEC-001', NULL, 'Hardware Technician Bogota',    'HARDWARE', 'BOGOTA',   3, 'ACTIVO'),
  ('TEC-002', NULL, 'Hardware Technician Medellin',  'HARDWARE', 'MEDELLIN', 3, 'ACTIVO'),
  ('TEC-003', NULL, 'Hardware Technician Cali',      'HARDWARE', 'CALI',     3, 'ACTIVO'),
  ('TEC-004', NULL, 'Software Technician Bogota',    'SOFTWARE', 'BOGOTA',   4, 'ACTIVO'),
  ('TEC-005', NULL, 'Software Technician Medellin',  'SOFTWARE', 'MEDELLIN', 4, 'ACTIVO'),
  ('TEC-006', NULL, 'Software Technician Cali',      'SOFTWARE', 'CALI',     4, 'ACTIVO'),
  ('TEC-007', NULL, 'Network Technician Bogota',     'RED',      'BOGOTA',   3, 'ACTIVO'),
  ('TEC-008', NULL, 'Network Technician Medellin',   'RED',      'MEDELLIN', 3, 'ACTIVO'),
  ('TEC-009', NULL, 'Network Technician Cali',       'RED',      'CALI',     3, 'ACTIVO'),
  ('TEC-010', NULL, 'Access Technician Bogota',      'ACCESOS',  'BOGOTA',   5, 'ACTIVO'),
  ('TEC-011', NULL, 'Access Technician Medellin',    'ACCESOS',  'MEDELLIN', 5, 'ACTIVO'),
  ('TEC-012', NULL, 'Access Technician Cali',        'ACCESOS',  'CALI',     5, 'ACTIVO'),
  ('TEC-013', NULL, 'General Technician Bogota',     'OTRO',     'BOGOTA',   5, 'ACTIVO'),
  ('TEC-014', NULL, 'General Technician Medellin',   'OTRO',     'MEDELLIN', 5, 'ACTIVO'),
  ('TEC-015', NULL, 'General Technician Cali',       'OTRO',     'CALI',     5, 'ACTIVO');
