ALTER TABLE BEHANDLING_DVH ADD MOTTATT_TIDSPUNKT TIMESTAMP(3);

COMMENT ON COLUMN BEHANDLING_DVH.MOTTATT_TIDSPUNKT IS 'Tidspunkt når søknad er mottatt';
