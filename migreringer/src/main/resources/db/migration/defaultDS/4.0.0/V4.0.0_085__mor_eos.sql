ALTER TABLE SO_RETTIGHET ADD MOR_STONAD_EOS VARCHAR2(1 CHAR) DEFAULT 'N' ;
COMMENT ON COLUMN SO_RETTIGHET.MOR_STONAD_EOS IS 'Oppgitt at mor mottar foreldrestønad fra land i EØS';

ALTER TABLE GR_YTELSES_FORDELING ADD MOR_STONAD_EOS_ID NUMBER ;
COMMENT ON COLUMN GR_YTELSES_FORDELING.MOR_STONAD_EOS_ID IS 'FK: Fremmednøkkel for kobling til YF_DOKUMENTASJON_PERIODER';
ALTER TABLE GR_YTELSES_FORDELING ADD CONSTRAINT FK_YF_DOKUMENTASJON_PERIODE_15 FOREIGN KEY (MOR_STONAD_EOS_ID) REFERENCES YF_DOKUMENTASJON_PERIODER (ID) ENABLE;

CREATE INDEX IDX_GR_YTELSES_FORDELING_19 ON GR_YTELSES_FORDELING (MOR_STONAD_EOS_ID)  ;