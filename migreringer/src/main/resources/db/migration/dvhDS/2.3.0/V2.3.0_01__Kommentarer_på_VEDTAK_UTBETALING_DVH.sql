COMMENT ON TABLE VEDTAK_UTBETALING_DVH  IS 'En tabell med med informasjon om alle vedtak fattet i VL, inkluderer utbetalingsinfo';
COMMENT ON COLUMN VEDTAK_UTBETALING_DVH.TRANS_ID IS 'Primær nøkkel';
COMMENT ON COLUMN VEDTAK_UTBETALING_DVH.TRANS_TID IS 'Timestamp som forteller nå transaksjonen inntraff.';
COMMENT ON COLUMN VEDTAK_UTBETALING_DVH.ENDRET_AV IS 'Opprettet_av eller endret_av i VL';
COMMENT ON COLUMN VEDTAK_UTBETALING_DVH.FUNKSJONELL_TID IS 'Et tidsstempel når transaksjonen er funksjonelt gyldig fra.';
COMMENT ON COLUMN VEDTAK_UTBETALING_DVH.BEHANDLING_ID IS 'Id til Behandling.';
COMMENT ON COLUMN VEDTAK_UTBETALING_DVH.FAGSAK_ID IS 'Id til Fagsak.';
COMMENT ON COLUMN VEDTAK_UTBETALING_DVH.VEDTAK_ID IS 'Id til Vedtak.';
COMMENT ON COLUMN VEDTAK_UTBETALING_DVH.VEDTAK_DATO IS 'Dato vedtaket ble fattet.';
COMMENT ON COLUMN VEDTAK_UTBETALING_DVH.XML_CLOB IS 'XML for Vedtak og utbetaling.';
COMMENT ON COLUMN VEDTAK_UTBETALING_DVH.SOEKNAD_TYPE IS 'Type søknad, fødsel eller adopsjon.';
COMMENT ON COLUMN VEDTAK_UTBETALING_DVH.BEHANDLING_TYPE IS 'Type behandling.';
