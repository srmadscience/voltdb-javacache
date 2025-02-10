--
-- Copyright (C) 2025 Volt Active Data Inc.
--
-- Use of this source code is governed by an MIT
-- license that can be found in the LICENSE file or at
-- https://opensource.org/licenses/MIT.
--

ALTER TABLE kv
ADD 
COLUMN create_date timestamp default now not null;

CREATE INDEX kv_ttl_idx ON kv(create_date);

ALTER TABLE kv
ALTER
USING TTL 1 SECONDS ON COLUMN create_date;


