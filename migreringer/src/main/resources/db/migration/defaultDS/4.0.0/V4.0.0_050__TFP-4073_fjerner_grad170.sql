DROP TABLE OKO_GRAD_170;
DROP SEQUENCE SEQ_OKO_GRAD_170;

ALTER TABLE OKO_OPPDRAG_LINJE_150 MODIFY (FRADRAG_TILLEGG null);
ALTER TABLE OKO_OPPDRAG_LINJE_150 MODIFY (BRUK_KJORE_PLAN null);
ALTER TABLE OKO_OPPDRAG_LINJE_150 MODIFY (HENVISNING null);
ALTER TABLE OKO_OPPDRAG_LINJE_150 MODIFY (VERSJON null);
ALTER TABLE OKO_OPPDRAG_LINJE_150 MODIFY (VEDTAK_ID not null);
ALTER TABLE OKO_OPPDRAG_LINJE_150 MODIFY (DATO_VEDTAK_TOM not null);
