DROP TABLE IF EXISTS "User_Team";
DROP TABLE IF EXISTS "Team";
DROP TABLE IF EXISTS "User";

DROP TABLE IF EXISTS "Task_CollaboratorNeed";
DROP TABLE IF EXISTS "Task_MaterialNeed";
DROP TABLE IF EXISTS "MaterialNeed";
DROP TABLE IF EXISTS "CollaboratorNeed";

DROP TYPE IF EXISTS "UserRole";
DROP TYPE IF EXISTS "Material";

DROP TABLE IF EXISTS "Task";
DROP TYPE IF EXISTS "TaskPriority";
DROP TYPE IF EXISTS "TaskDeadline";

DROP TABLE IF EXISTS "Result";
DROP TABLE IF EXISTS "Goal";
DROP TABLE IF EXISTS "Project";

DROP TRIGGER IF EXISTS check_circular_dependency ON "Task_Subtask";
DROP FUNCTION IF EXISTS check_task_subtask_relation;

DROP TRIGGER IF EXISTS check_on_task_done ON "Task";
DROP FUNCTION IF EXISTS check_dependencies_on_task_done;

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

CREATE TABLE "Project"(
	id SERIAL,
	name VARCHAR(100) NOT NULL,
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
	CONSTRAINT UC_User_email UNIQUE(email)
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
	id SERIAL,
	userId INT NOT NULL,
	teamId INT NOT NULL,
	CONSTRAINT UC_User_Team UNIQUE (userId, teamId),
	CONSTRAINT PK_User_Team PRIMARY KEY(id),
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

-- Check for circular dependencies
CREATE OR REPLACE FUNCTION check_task_subtask_relation()
RETURNS TRIGGER AS $$
DECLARE
    circular_dependency_exists BOOLEAN := FALSE;
BEGIN
    SELECT EXISTS(
        WITH RECURSIVE Subtasks AS (
            SELECT subtaskid FROM "Task_Subtask" WHERE taskid = NEW.taskid

            UNION ALL
              SELECT ts.subtaskid
              FROM "Task_Subtask" ts
              INNER JOIN Subtasks st ON st.subtaskid = ts.taskid
        )
        SELECT 1 FROM Subtasks WHERE subtaskid = NEW.subtaskid
    ) INTO circular_dependency_exists;

    IF circular_dependency_exists THEN
        RAISE EXCEPTION 'Cannot insert: circular dependency detected. Task % is already a subtask of %.', NEW.subtaskid, NEW.taskid;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER check_circular_dependency
BEFORE INSERT ON "Task_Subtask"
FOR EACH ROW
EXECUTE FUNCTION check_task_subtask_relation();

-- check for dependencies when marking task as done
CREATE OR REPLACE FUNCTION check_dependencies_on_task_done()
RETURNS TRIGGER AS $$
DECLARE
    pending_required_dependencies BOOLEAN := FALSE;
BEGIN
    SELECT EXISTS(
        SELECT 1 AS exist
        FROM "Task_Subtask" ts
		INNER JOIN "Task" st ON ts.subtaskId = st.id
        WHERE ts.taskId = NEW.id 
          AND ts.required IS TRUE 
          AND st.done IS NOT TRUE
    ) INTO pending_required_dependencies;

    IF pending_required_dependencies THEN
        RAISE EXCEPTION 'Cannot mark task as done: direct dependencies still not done.';
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
    dependent_task_ids TEXT; -- Variable to store all dependent task IDs as a comma-separated string
BEGIN
    -- Check for all tasks requiring the current task (OLD.id) that are not done
    SELECT STRING_AGG(id::TEXT, ', ')
    INTO dependent_task_ids
    FROM "Task"
    WHERE requiredTaskId = OLD.id AND done = FALSE;

    IF dependent_task_ids IS NOT NULL THEN
        RAISE EXCEPTION 'Task cannot be deleted, still required by the following tasks: %.', dependent_task_ids;
    END IF;

    RETURN OLD;
END;
$$ LANGUAGE plpgsql;



CREATE OR REPLACE TRIGGER taskDeletion 
BEFORE DELETE ON Task 
FOR EACH ROW 
EXECUTE FUNCTION check_task_deletion();
