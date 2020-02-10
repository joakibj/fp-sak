-- setter opp lese -og skriverettigheter til DVH-tabeller

REVOKE ALL ON FAGSAK_DVH FROM FPSAK_HIST_SKRIVE_ROLE;
GRANT INSERT ON FAGSAK_DVH TO FPSAK_HIST_SKRIVE_ROLE;
GRANT SELECT ON FAGSAK_DVH TO FPSAK_HIST_LESE_ROLE;

REVOKE ALL ON BEHANDLING_DVH FROM FPSAK_HIST_SKRIVE_ROLE;
GRANT INSERT ON BEHANDLING_DVH TO FPSAK_HIST_SKRIVE_ROLE;
GRANT SELECT ON BEHANDLING_DVH TO FPSAK_HIST_LESE_ROLE;

REVOKE ALL ON BEHANDLING_STEG_DVH FROM FPSAK_HIST_SKRIVE_ROLE;
GRANT INSERT ON BEHANDLING_STEG_DVH TO FPSAK_HIST_SKRIVE_ROLE;
GRANT SELECT ON BEHANDLING_STEG_DVH TO FPSAK_HIST_LESE_ROLE;

REVOKE ALL ON AKSJONSPUNKT_DVH FROM FPSAK_HIST_SKRIVE_ROLE;
GRANT INSERT ON AKSJONSPUNKT_DVH TO FPSAK_HIST_SKRIVE_ROLE;
GRANT SELECT ON AKSJONSPUNKT_DVH TO FPSAK_HIST_LESE_ROLE;

REVOKE ALL ON BEHANDLING_VEDTAK_DVH FROM FPSAK_HIST_SKRIVE_ROLE;
GRANT INSERT ON BEHANDLING_VEDTAK_DVH TO FPSAK_HIST_SKRIVE_ROLE;
GRANT SELECT ON BEHANDLING_VEDTAK_DVH TO FPSAK_HIST_LESE_ROLE;

REVOKE ALL ON KONTROLL_DVH FROM FPSAK_HIST_SKRIVE_ROLE;
GRANT SELECT, INSERT ON KONTROLL_DVH TO FPSAK_HIST_SKRIVE_ROLE;
GRANT SELECT, INSERT ON KONTROLL_DVH TO FPSAK_HIST_LESE_ROLE;
