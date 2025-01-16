-- Peuplement de la table "User"
INSERT INTO "User" (firstname, lastname, email, role) VALUES
('Alice', 'Martin', 'alice.martin@example.com', 'ADMIN'),
('Bob', 'Smith', 'bob.smith@example.com', 'MANAGER'),
('Charlie', 'Brown', 'charlie.brown@example.com', 'DEVELOPER'),
('Diana', 'Prince', 'diana.prince@example.com', 'SCRUM_MASTER'),
('Eve', 'Adams', 'eve.adams@example.com', 'CONTRIBUTOR'),
('Frank', 'Jones', 'frank.jones@example.com', 'DATA_SPECIALIST');

-- Peuplement de la table "Team" sans manager pour le moment
INSERT INTO "Team" (name) VALUES
('Alpha Team'),
('Beta Team');

-- Peuplement de la table "User_Team" pour ajouter les utilisateurs aux équipes
INSERT INTO "User_Team" (userId, teamId) VALUES
(1, 1), -- Alice dans Alpha Team
(2, 1), -- Bob dans Alpha Team
(3, 1), -- Charlie dans Alpha Team
(4, 2), -- Diana dans Beta Team
(5, 2), -- Eve dans Beta Team
(6, 2); -- Frank dans Beta Team

-- Mise à jour des managers dans la table "Team"
UPDATE "Team"
SET managerId = 2 -- Bob est manager de l'équipe Alpha
WHERE id = 1;

UPDATE "Team"
SET managerId = 4 -- Diana est manager de l'équipe Beta
WHERE id = 2;

-- Peuplement de la table "Project"
INSERT INTO "Project" (name) VALUES
('Project Apollo'),
('Project Orion');

-- Peuplement de la table "Goal"
INSERT INTO "Goal" (name, description, note, tag, projectId, teamId) VALUES
('Launch Rocket', 'Prepare for rocket launch', 'High priority', 'space,rocket', 1, 1),
('Develop AI Module', 'AI system for analysis', 'Critical', 'ai,development', 2, 2);

-- Peuplement de la table "Result"
INSERT INTO "Result" (title, createdAt, endsAt, note, tag, goalId) VALUES
('Rocket Prototype Completed', '2025-01-01', '2025-03-01', 'Prototype ready', 'hardware,rocket', 1),
('AI Module Beta', '2025-01-15', '2025-06-15', 'Beta version released', 'software,ai', 2);

-- Peuplement de la table "Task"
INSERT INTO "Task" (title, startsAt, done, priority, deadline, note, tag, resultId) VALUES
('Assemble Rocket Components', '2025-02-01', FALSE, 'HIGH', 'THREE_MONTHS', 'Assembly needed', 'rocket,assembly', 1),
('Test AI Algorithms', '2025-03-01', FALSE, 'MEDIUM', 'ONE_YEAR', 'Run tests on datasets', 'ai,testing', 2);

-- Peuplement de la table "Task_Subtask"
INSERT INTO "Task_Subtask" (taskId, subtaskId, required) VALUES
(1, 2, TRUE);

-- Peuplement de la table "Task_CollaboratorNeed"
INSERT INTO "Task_CollaboratorNeed" (taskId, collaboratorNeedType, quantity) VALUES
(1, 'DEVELOPER', 3),
(2, 'DATA_SPECIALIST', 2);

-- Peuplement de la table "Task_MaterialNeed"
INSERT INTO "Task_MaterialNeed" (taskId, materialNeedType, quantity) VALUES
(1, 'SERVER', 1),
(2, 'DATABASE', 1);

