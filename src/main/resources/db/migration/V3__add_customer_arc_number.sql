-- 고객에게 최초 공유된 Arc 순번을 고객별로 고정해 저장하는 migration
ALTER TABLE arcs ADD COLUMN arc_number INT NULL;

ALTER TABLE arcs
    ADD CONSTRAINT uk_arcs_customer_arc_number UNIQUE (customer_id, arc_number);
