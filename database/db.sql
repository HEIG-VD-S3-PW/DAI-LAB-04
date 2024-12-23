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
DROP TYPE IF EXISTS "TaskDeadLine";

DROP TABLE IF EXISTS "Result";
DROP TABLE IF EXISTS "Goal";
DROP TABLE IF EXISTS "Project";

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
CREATE TYPE "TaskDeadLine" AS ENUM ('3_MONTHS', '1_YEAR', '3_YEARS');
CREATE TABLE "Task"(
	id SERIAL,
	startsAt TIMESTAMP NOT NULL,
	progress SMALLINT CHECK (progress >= 0 AND progress <= 100),
	priority "TaskPriority" DEFAULT 'MEDIUM',
	deadline "TaskDeadLine" DEFAULT '3_MONTHS',
	note TEXT,
	tag TEXT,
	isRequired BOOL DEFAULT FALSE,
	parentTaskId INT NULL,
	resultId INT NOT NULL,
	CONSTRAINT PK_Task PRIMARY KEY(id),
	CONSTRAINT UC_Task_starts_at UNIQUE(startsAt),
	CONSTRAINT FK_Task_parentTaskId FOREIGN KEY (parentTaskId) REFERENCES "Task"(id) ON DELETE SET NULL ON UPDATE CASCADE,
	CONSTRAINT FK_Task_resultId FOREIGN KEY (resultId) REFERENCES "Result"(id) ON DELETE CASCADE ON UPDATE CASCADE
);

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
