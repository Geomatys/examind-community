
-- Increase some column varchar length
ALTER TABLE admin.cstl_user
    ALTER COLUMN login TYPE varchar(255),
    ALTER COLUMN firstname TYPE varchar(255),
    ALTER COLUMN lastname TYPE varchar(255),
    ALTER COLUMN email TYPE varchar(255),
    ALTER COLUMN avatar TYPE varchar(255),
    ALTER COLUMN city TYPE varchar(255),
    ALTER COLUMN country TYPE varchar(255);
