ALTER TABLE SO_RETTIGHET DROP CONSTRAINT CHK_SO_RETTIGHET_01;
ALTER TABLE SO_RETTIGHET DROP CONSTRAINT CHK_SO_RETTIGHET_02;
ALTER TABLE SO_RETTIGHET DROP CONSTRAINT CHK_SO_RETTIGHET_03;

ALTER TABLE SO_RETTIGHET set unused (omsorg_i_hele_perioden);