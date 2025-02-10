--
-- Copyright (C) 2025 Volt Active Data Inc.
--
-- Use of this source code is governed by an MIT
-- license that can be found in the LICENSE file or at
-- https://opensource.org/licenses/MIT.
--

load classes ../jars/cache-api-1.1.1.jar;

load classes ../jars/voltdb-javacache.jar;

file -inlinebatch END_OF_BATCH

CREATE table kv_parameters
(c varchar(30) not null 
,param_name varchar(30) not null 
,param_value bigint not null
,primary key (c, param_name)
);

CREATE TABLE kv 
(c varchar(30) not null 
,k varchar(128) not null 
,v varbinary(1048576)
,primary key (c, k));

PARTITION TABLE kv ON COLUMN k;

CREATE STREAM kv_deltas 
PARTITION ON COLUMN k 
EXPORT TO TOPIC kv_deltas_topic
WITH KEY (c,k) VALUE (c,k,v)
(c varchar(30) not null 
,k varchar(128)  not null
,v varbinary(1048576)
,event_type varchar(1));

CREATE PROCEDURE 
ContainsKey
PARTITION ON TABLE kv COLUMN k
AS
SELECT 'x' found 
FROM kv
WHERE k = ? 
AND   c = ?;


CREATE PROCEDURE 
Get
PARTITION ON TABLE kv COLUMN k
AS
SELECT v
FROM kv
WHERE k = ? 
AND   c = ?;

CREATE PROCEDURE 
GetKV
PARTITION ON TABLE kv COLUMN k
AS
SELECT k, v 
FROM kv
WHERE k = ?
AND   c = ?;

-- WARNING: This might return more data than VoltDB 
-- client can handle...
CREATE PROCEDURE 
Iterator 
AS
SELECT * FROM kv WHERE c = ? ORDER BY k;

CREATE PROCEDURE 
GetParam
AS 
SELECT param_value
FROM kv_parameters 
WHERE c = ? 
AND   param_name = ?;

CREATE PROCEDURE 
PARTITION ON TABLE kv COLUMN k
FROM CLASS jsr107.RemoveKeyValuePair;

CREATE PROCEDURE 
PARTITION ON TABLE kv COLUMN k
FROM CLASS jsr107.Remove;

CREATE PROCEDURE 
FROM CLASS jsr107.RemoveAll;

CREATE PROCEDURE 
PARTITION ON TABLE kv COLUMN k
FROM CLASS jsr107.Put;

CREATE PROCEDURE 
PARTITION ON TABLE kv COLUMN k
FROM CLASS jsr107.GetAndPut;

CREATE PROCEDURE 
PARTITION ON TABLE kv COLUMN k
FROM CLASS jsr107.GetAndRemove;


CREATE PROCEDURE 
PARTITION ON TABLE kv COLUMN k
FROM CLASS jsr107.GetAndReplace;


CREATE PROCEDURE 
PARTITION ON TABLE kv COLUMN k
FROM CLASS jsr107.PutIfAbsent;


CREATE PROCEDURE 
PARTITION ON TABLE kv COLUMN k
FROM CLASS jsr107.Replace;

CREATE PROCEDURE 
PARTITION ON TABLE kv COLUMN k
FROM CLASS jsr107.ReplaceKeyValuePair;

CREATE PROCEDURE 
PARTITION ON TABLE kv COLUMN k
FROM CLASS jsr107.Invoke;


END_OF_BATCH

UPSERT INTO kv_parameters VALUES ('Test','ENABLE_EVENTS',1);

