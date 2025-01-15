-- Insert basic Users
INSERT INTO "User" (firstname, lastname, email, role) VALUES
('Alice', 'Admin', 'alice.admin@example.com', 'ADMIN'),
('Bob', 'Manager', 'bob.manager@example.com', 'MANAGER'),
('Charlie', 'Dev', 'charlie.dev@example.com', 'DEVELOPER'),
('Diana', 'Scrum', 'diana.scrum@example.com', 'SCRUM_MASTER'),
('Eve', 'Contributor', 'eve.contributor@example.com', 'CONTRIBUTOR');

-- Insert Teams
INSERT INTO "Team" (name, managerId) VALUES
('Alpha Team', 2), -- Bob is the manager
('Beta Team', NULL); -- No manager yet

-- Add Users to Teams
INSERT INTO "User_Team" (userId, teamId) VALUES
(1, 1), -- Alice in Alpha Team
(2, 1), -- Bob in Alpha Team
(3, 1), -- Charlie in Alpha Team
(4, 2), -- Diana in Beta Team
(5, 2); -- Eve in Beta Team

-- Insert Projects
INSERT INTO "Project" (name) VALUES
('Project Apollo'),
('Project Beta');

-- Insert Goals
INSERT INTO "Goal" (name, description, note, tag, projectId, teamId) VALUES
('Complete Backend', 'Finish backend development tasks.', 'Priority is high.', 'Backend', 1, 1),
('Design Frontend', 'Finalize the UI/UX design.', 'Design should be intuitive.', 'Frontend', 2, 2);

-- Insert Results
INSERT INTO "Result" (title, createdAt, endsAt, note, tag, goalId) VALUES
('Backend MVP', '2025-01-01', '2025-03-01', 'Initial version of backend.', 'MVP', 1),
('Frontend Wireframe', '2025-01-01', '2025-02-15', 'Draft of UI design.', 'Wireframe', 2);

-- Insert Tasks
INSERT INTO "Task" (title, startsAt, priority, deadline, note, tag, resultId) VALUES
('Set up Database', '2025-01-05', 'HIGH', 'THREE_MONTHS', 'Initialize DB schema.', 'DB', 1),
('Create API Endpoints', '2025-01-10', 'MEDIUM', 'THREE_MONTHS', 'Develop core APIs.', 'API', 1),
('Design Landing Page', '2025-01-07', 'LOW', 'ONE_YEAR', 'Work on landing page design.', 'Landing', 2),
('Develop Wireframe', '2025-01-08', 'MEDIUM', 'ONE_YEAR', 'Detailed wireframe.', 'Wireframe', 2);

-- Insert Task Dependencies (Subtasks)
INSERT INTO "Task_Subtask" (taskId, subtaskId, required) VALUES
(1, 2, TRUE), -- "Set up Database" is required for "Create API Endpoints"
(3, 4, TRUE); -- "Design Landing Page" is required for "Develop Wireframe"

-- Insert Material Needs for Tasks
INSERT INTO "Task_MaterialNeed" (taskId, materialNeedType, quantity) VALUES
(1, 'DATABASE', 1), -- Task 1 requires a database
(2, 'SERVER', 1); -- Task 2 requires a server

-- Insert Collaborator Needs for Tasks
INSERT INTO "Task_CollaboratorNeed" (taskId, collaboratorNeedType, quantity) VALUES
(1, 'DEVELOPER', 2), -- Task 1 needs 2 developers
(2, 'DEVELOPER', 1); -- Task 2 needs 1 developer

