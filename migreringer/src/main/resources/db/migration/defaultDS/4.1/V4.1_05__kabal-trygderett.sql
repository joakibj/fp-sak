ALTER TABLE ANKE_VURDERING_RESULTAT ADD SENDT_TRYGDERETT_DATO DATE ;
COMMENT ON COLUMN ANKE_VURDERING_RESULTAT.SENDT_TRYGDERETT_DATO IS 'Dato anke sendt til Trygderetten';

alter table SO_RETTIGHET set unused (mor_stonad_eos);
