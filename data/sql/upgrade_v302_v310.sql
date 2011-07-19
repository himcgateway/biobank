ALTER TABLE shipment_info
      ADD COLUMN COMMENT TEXT CHARACTER SET latin1 COLLATE latin1_general_cs NULL DEFAULT NULL COMMENT '';

ALTER TABLE request DROP FOREIGN KEY FK6C1A7E6F80AB67E;
ALTER TABLE request DROP INDEX FK6C1A7E6F80AB67E, DROP COLUMN STATE, DROP COLUMN REQUESTER_ID;

ALTER TABLE request_specimen DROP FOREIGN KEY FK579572D8D990A70;
ALTER TABLE request_specimen DROP INDEX FK579572D8D990A70, DROP COLUMN AREQUEST_ID;
ALTER TABLE request_specimen ADD COLUMN REQUEST_ID INT(11) NOT NULL COMMENT '', ADD INDEX FK579572D8A2F14F4F (REQUEST_ID);
ALTER TABLE request_specimen ADD CONSTRAINT FK579572D8A2F14F4F FOREIGN KEY FK579572D8A2F14F4F (REQUEST_ID) REFERENCES request (ID) ON UPDATE NO ACTION ON DELETE NO ACTION;

-- add country field and set all existing ones to Canada (issue #1237)
alter table ADDRESS add COLUMN COUNTRY varchar(50);
update ADDRESS set COUNTRY = 'Canada';


-- update discriminator of SpecimenPosition (before was AliquotPosition)
update abstract_position
set discriminator = 'SpecimenPosition'
where discriminator = 'AliquotPosition';

