-- Un solo plan vigente por carrera (defensa real ante carreras entre las réplicas del backend)
CREATE UNIQUE INDEX IF NOT EXISTS uk_study_plan_one_current ON study_plan (career_id) WHERE status = 'CURRENT';
