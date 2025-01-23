DROP TABLE IF EXISTS "Task_CollaboratorNeed";
DROP TABLE IF EXISTS "Task_MaterialNeed";
DROP TABLE IF EXISTS "MaterialNeed";
DROP TABLE IF EXISTS "CollaboratorNeed";
DROP TABLE IF EXISTS "Task_Subtask";
DROP TABLE IF EXISTS "Task";
DROP TYPE IF EXISTS "TaskPriority";
DROP TYPE IF EXISTS "TaskDeadline";

DROP TRIGGER IF EXISTS prevent_circular_dependencies ON "Task_Subtask";
DROP FUNCTION IF EXISTS check_task_subtask_relation;

DROP TRIGGER IF EXISTS check_on_task_done ON "Task";
DROP FUNCTION IF EXISTS check_dependencies_on_task_done;

DROP TRIGGER IF EXISTS task_deletion ON "Task";
DROP FUNCTION IF EXISTS check_task_deletion;

DROP TRIGGER IF EXISTS check_task_dependencies ON "Task_Subtask";
DROP FUNCTION IF EXISTS task_dependencies;

DROP TABLE IF EXISTS "Result";
DROP TABLE IF EXISTS "Goal";
DROP TABLE IF EXISTS "Project";
DROP TABLE IF EXISTS "User_Team";
DROP TABLE IF EXISTS "Team";
DROP TABLE IF EXISTS "User";

DROP TYPE IF EXISTS "UserRole";
DROP TYPE IF EXISTS "Material";

CREATE TYPE "UserRole" AS ENUM ('ADMIN', 'MANAGER', 'CONTRIBUTOR', 'DEVELOPER', 'SCRUM_MASTER', 'DATA_SPECIALIST');
CREATE Type "Material" AS ENUM('LICENSE', 'SERVER', 'DATABASE'); 

CREATE TABLE "MaterialNeed"(
	type "Material" NOT NULL,
	CONSTRAINT PK_MaterialNeed PRIMARY KEY(type)
);

CREATE TABLE "CollaboratorNeed"(
	type "UserRole" NOT NULL,
	CONSTRAINT PK_CollaboratorNeed PRIMARY KEY(type)
);

INSERT INTO "CollaboratorNeed" ("type") VALUES
                                            ('ADMIN'),
                                            ('MANAGER'),
                                            ('CONTRIBUTOR'),
                                            ('DEVELOPER'),
                                            ('SCRUM_MASTER'),
                                            ('DATA_SPECIALIST');

INSERT INTO "MaterialNeed" ("type") VALUES
                                            ('LICENSE'),
                                            ('SERVER'),
                                            ('DATABASE');

CREATE TABLE "Project"(
	id SERIAL,
	name VARCHAR(100) NOT NULL,
    description TEXT,
	CONSTRAINT PK_Project PRIMARY KEY(id),
	CONSTRAINT UC_Project_name UNIQUE(name)
);

CREATE TABLE "User"(
	id SERIAL,
	firstname VARCHAR(100) NOT NULL,
	lastname VARCHAR(100) NOT NULL,
	email VARCHAR(255) NOT NULL,
	role "UserRole" DEFAULT 'CONTRIBUTOR',
	CONSTRAINT PK_User PRIMARY KEY(id),
	CONSTRAINT UC_User_email UNIQUE(email),
	CONSTRAINT CK_User_email CHECK (email ~* '^[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,}$')
);

CREATE TABLE "Team" (
	id SERIAL,
	name VARCHAR(50) NOT NULL,
	managerId INT NULL,
	CONSTRAINT PK_Team PRIMARY KEY(id),
	CONSTRAINT FK_Team_managerId FOREIGN KEY (managerId) REFERENCES "User"(id) ON DELETE SET NULL ON UPDATE CASCADE,
	CONSTRAINT UC_Team_name UNIQUE(name)
);

CREATE TABLE "User_Team" (
	userId INT NOT NULL,
	teamId INT NOT NULL,
	CONSTRAINT PK_User_Team PRIMARY KEY(userId, teamId),
	CONSTRAINT FK_User_Team_userId FOREIGN KEY (userId) REFERENCES "User"(id) ON DELETE CASCADE ON UPDATE CASCADE,
	CONSTRAINT FK_User_Team_teamId FOREIGN KEY (teamId) REFERENCES "Team"(id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE "Goal"(
	id SERIAL,
	name VARCHAR(100) NOT NULL,
	description TEXT,
	note TEXT,
	tag TEXT,
	projectId INT NOT NULL,
    teamId INT,
	CONSTRAINT PK_Goal PRIMARY KEY(id),
	CONSTRAINT FK_Goal_projectId FOREIGN KEY (projectId) REFERENCES "Project"(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT FK_Goal_teamId FOREIGN KEY (teamId) REFERENCES "Team"(id) ON DELETE SET NULL ON UPDATE CASCADE,
	CONSTRAINT UC_Goal_name UNIQUE(name)
);
CREATE TABLE "Result"(
	id SERIAL,
    title VARCHAR(150) NOT NULL,
	createdAt TIMESTAMP NOT NULL DEFAULT NOW(),
	endsAt TIMESTAMP NULL,
	note TEXT,
	tag TEXT,
	goalId INT NOT NULL,
	CONSTRAINT PK_Result PRIMARY KEY(id),
	CONSTRAINT FK_Result_goalId FOREIGN KEY (goalId) REFERENCES "Goal"(id) ON DELETE CASCADE ON UPDATE CASCADE,
	CONSTRAINT CHK_Result_dates CHECK (createdAt < endsAt)
);

CREATE TYPE "TaskPriority" AS ENUM ('LOW', 'MEDIUM', 'HIGH');
CREATE TYPE "TaskDeadline" AS ENUM ('THREE_MONTHS', 'ONE_YEAR', 'THREE_YEARS');
CREATE TABLE "Task"(
	id SERIAL,
    title VARCHAR(150) NOT NULL,
	startsAt TIMESTAMP NOT NULL,
	done BOOLEAN NOT NULL DEFAULT FALSE,
	priority "TaskPriority" DEFAULT 'MEDIUM',
	deadline "TaskDeadline" DEFAULT 'THREE_MONTHS',
	note TEXT,
	tag TEXT,
	resultId INT NOT NULL,
	CONSTRAINT PK_Task PRIMARY KEY(id),
	CONSTRAINT UC_Task_starts_at UNIQUE(startsAt),
	CONSTRAINT FK_Task_resultId FOREIGN KEY (resultId) REFERENCES "Result"(id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE "Task_Subtask" (
	id SERIAL,
	taskId INT NOT NULL,
	subtaskId INT NOT NULL,
  	required BOOLEAN DEFAULT FALSE,
	CONSTRAINT PK_Task_Subtask PRIMARY KEY(id),
	CONSTRAINT FK_Task_taskId FOREIGN KEY (taskId) REFERENCES "Task"(id) ON DELETE CASCADE ON UPDATE CASCADE,
	CONSTRAINT FK_Task_subtaskId FOREIGN KEY (subtaskId) REFERENCES "Task"(id) ON DELETE CASCADE ON UPDATE CASCADE,
	CONSTRAINT UC_Task_taskId_subtaskId UNIQUE(taskId, subtaskId)
);

-- Function to find all subtasks recursively (including existing ones in DB)
CREATE OR REPLACE FUNCTION find_all_subtasks(start_id INTEGER, current_id INTEGER DEFAULT NULL)
RETURNS SETOF INTEGER AS $$
BEGIN
    -- Initialize current_id to start_id on first call
    IF current_id IS NULL THEN
        current_id := start_id;
    END IF;
    
    -- Return all subtasks recursively
    RETURN QUERY
        WITH RECURSIVE subtask_tree AS (
            -- Base case: direct subtasks
            SELECT ts.subtaskId, ts.taskId, ARRAY[ts.taskId] as path
            FROM "Task_Subtask" ts
            WHERE ts.taskId = current_id
            
            UNION ALL
            
            -- Recursive case: subtasks of subtasks
            SELECT ts.subtaskId, ts.taskId, st.path || ts.taskId
            FROM "Task_Subtask" ts
            INNER JOIN subtask_tree st ON ts.taskId = st.subtaskId
            WHERE NOT ts.taskId = ANY(st.path)  -- Prevent infinite recursion
        )
        SELECT DISTINCT subtaskId FROM subtask_tree;
END;
$$ LANGUAGE plpgsql;

-- Trigger function to check for cycles in dependencies
CREATE OR REPLACE FUNCTION check_circular_dependencies()
RETURNS TRIGGER AS $$
DECLARE
    task_id INTEGER;
    subtask_id INTEGER;
BEGIN
    -- Prevent self-reference
    IF NEW.taskId = NEW.subtaskId THEN
        RAISE EXCEPTION 'Task % cannot be its own subtask', NEW.taskId;
    END IF;
    
    -- Check if the new subtask has the task as one of its subtasks (would create a cycle)
    FOR task_id IN SELECT * FROM find_all_subtasks(NEW.subtaskId) LOOP
        IF task_id = NEW.taskId THEN
            RAISE EXCEPTION 'Circular dependency detected: Task % would create a cycle through subtask %', 
                          NEW.taskId, NEW.subtaskId;
        END IF;
    END LOOP;
    
    -- Also check if any of the existing subtasks would create a cycle
    FOR subtask_id IN SELECT * FROM find_all_subtasks(NEW.taskId) LOOP
        IF subtask_id = NEW.taskId THEN
            RAISE EXCEPTION 'Circular dependency detected: Task % is already a subtask of %', 
                          NEW.taskId, NEW.subtaskId;
        END IF;
    END LOOP;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER prevent_circular_dependencies
    BEFORE INSERT OR UPDATE ON "Task_Subtask"
    FOR EACH ROW
    EXECUTE FUNCTION check_circular_dependencies();


CREATE OR REPLACE FUNCTION check_dependencies_on_task_done()
RETURNS TRIGGER AS $$
DECLARE
pending_required_dependencies BOOLEAN := FALSE;
BEGIN
    -- Vérifier si des dépendances requises ne sont pas terminées
	WITH RECURSIVE TaskDependencies AS (
	    -- Départ : dépendances directes de la tâche
	    SELECT ts.subtaskId
	    FROM "Task_Subtask" ts
	    WHERE ts.taskId = NEW.id AND ts.required IS TRUE
	
	    UNION ALL
	
	    -- Récursion : dépendances indirectes
	    SELECT ts.subtaskId
	    FROM "Task_Subtask" ts
	             INNER JOIN TaskDependencies td ON ts.taskId = td.subtaskId
	    WHERE ts.required IS TRUE
	)
	SELECT EXISTS(
	    SELECT 1
	    FROM TaskDependencies td
	             INNER JOIN "Task" t ON td.subtaskId = t.id
	    WHERE t.done IS NOT TRUE
	) INTO pending_required_dependencies;
	
	-- Empêche de mettre une task "undone" si elle est la dépendance d'une tâche terminée
	
	IF (OLD.done = TRUE AND NEW.done = FALSE) THEN
	    IF EXISTS (
	        SELECT 1
	        FROM "Task_Subtask" ts
	        INNER JOIN "Task" t ON t.id = ts.taskId
	        WHERE ts.subtaskId = NEW.id AND t.done = TRUE
	    ) THEN
	        RAISE EXCEPTION 'Cannot mark the task undone, its parent task is already done.';
	    END IF;
	END IF;
	
	-- Si des dépendances ne sont pas terminées, lever une exception
	IF pending_required_dependencies THEN
	        RAISE EXCEPTION 'Cannot mark task as done: dependencies still not done.';
	END IF;

RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER check_on_task_done
    BEFORE UPDATE ON "Task"
    FOR EACH ROW
    WHEN (OLD.done IS DISTINCT FROM NEW.done)
EXECUTE FUNCTION check_dependencies_on_task_done();

CREATE TABLE "Task_CollaboratorNeed" (
	taskId INT NOT NULL,
	collaboratorNeedType "UserRole" NOT NULL,
	quantity INT NOT NULL,
	CONSTRAINT PK_Task_CollaboratorNeed PRIMARY KEY(taskId, collaboratorNeedType),
	CONSTRAINT FK_Task_CollaboratorNeed_taskId FOREIGN KEY (taskId) REFERENCES "Task"(id) ON DELETE CASCADE ON UPDATE CASCADE,
	CONSTRAINT FK_Task_CollaboratorNeed_collaboratorNeedType FOREIGN KEY (collaboratorNeedType) REFERENCES "CollaboratorNeed"(type) ON DELETE CASCADE ON UPDATE CASCADE,
	CONSTRAINT CHK_Task_CollaboratorNeed_quantity0 CHECK (quantity > 0)
);

CREATE TABLE "Task_MaterialNeed" (
	taskId INT NOT NULL,
	materialNeedType "Material" NOT NULL,
	quantity INT NOT NULL,
	CONSTRAINT PK_Task_MaterialNeed PRIMARY KEY(taskId, materialNeedType),
	CONSTRAINT FK_Task_MaterialNeed_taskId FOREIGN KEY (taskId) REFERENCES "Task"(id) ON DELETE CASCADE ON UPDATE CASCADE,
	CONSTRAINT FK_Task_MaterialNeed_materialNeedType FOREIGN KEY (materialNeedType) REFERENCES "MaterialNeed"(type) ON DELETE CASCADE ON UPDATE CASCADE,
	CONSTRAINT CHK_Task_MaterialNeed_quantity CHECK (quantity > 0)
);


-- CI: Un manager doit être membre de l'équipe avant de devenir manager
CREATE OR REPLACE FUNCTION check_manager_belongs_to_team()
RETURNS TRIGGER AS $$
BEGIN
    -- Check if the manager belongs to the team
    IF NOT EXISTS (
        SELECT 1
        FROM "User_Team"
        WHERE "User_Team".userId = NEW.managerId AND "User_Team".teamId = NEW.id
    ) THEN
        RAISE EXCEPTION 'Manager must belong to the team before becoming its manager';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS check_manager_trigger ON "Team";
CREATE TRIGGER check_manager_trigger
BEFORE INSERT OR UPDATE ON "Team"
FOR EACH ROW
WHEN (NEW.managerId IS NOT NULL)
EXECUTE FUNCTION check_manager_belongs_to_team();

-- CI : Une tâche doit avoir une date de début entre la création et la deadline de son résultat associé
CREATE OR REPLACE FUNCTION check_task_dates()
RETURNS TRIGGER AS $$
BEGIN
	-- Check if the task start date is between the result creation and end dates
    IF EXISTS (
        SELECT 1
        FROM "Result" r
        WHERE r.id = NEW.resultId 
        AND (NEW.startsAt < r.createdAt OR NEW.startsAt > r.endsAt)
    ) THEN
        RAISE EXCEPTION 'Task start date must be between Result creation and end dates';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER check_task_dates_trigger
BEFORE INSERT OR UPDATE ON "Task"
FOR EACH ROW
EXECUTE FUNCTION check_task_dates();

-- check when deleting a task if it is the requiredTask of another one that isn't done yet
CREATE OR REPLACE FUNCTION check_task_deletion()
RETURNS TRIGGER AS $$
DECLARE
    dependent_task_ids TEXT;
BEGIN
    -- Check for any tasks requiring the current task as a subtask that are not done
    SELECT STRING_AGG(t.id::TEXT, ', ')
    INTO dependent_task_ids
    FROM "Task_Subtask" ts
    INNER JOIN "Task" t ON ts.taskId = t.id
    WHERE ts.subtaskId = OLD.id AND t.done = FALSE;

    IF dependent_task_ids IS NOT NULL THEN
        RAISE EXCEPTION 'Task cannot be deleted, still required by the following tasks: %.', dependent_task_ids;
    END IF;

    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER task_deletion 
BEFORE DELETE ON "Task"
FOR EACH ROW 
EXECUTE FUNCTION check_task_deletion();


CREATE OR REPLACE FUNCTION task_dependencies()
RETURNS TRIGGER AS $$
DECLARE
    task_project_id INT;
    subtask_project_id INT;
BEGIN
    -- Get the project ID for the main task
    SELECT g.projectId
    INTO task_project_id
    FROM "Task" t
    INNER JOIN "Result" r ON t.resultId = r.id
    INNER JOIN "Goal" g ON r.goalId = g.id
    WHERE t.id = NEW.taskId;

    -- Get the project ID for the subtask
    SELECT g.projectId
    INTO subtask_project_id
    FROM "Task" t
    INNER JOIN "Result" r ON t.resultId = r.id
    INNER JOIN "Goal" g ON r.goalId = g.id
    WHERE t.id = NEW.subtaskId;

    -- Compare project IDs
    IF task_project_id IS DISTINCT FROM subtask_project_id THEN
        RAISE EXCEPTION 'Task and subtask must belong to the same project. Task project: %, Subtask project: %',
            task_project_id, subtask_project_id;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;



CREATE OR REPLACE TRIGGER check_task_dependencies
BEFORE INSERT OR UPDATE ON "Task_Subtask"
FOR EACH ROW
EXECUTE FUNCTION task_dependencies();

