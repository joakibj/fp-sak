-- klage hjemmel
alter table KLAGE_VURDERING_RESULTAT_DVH ADD KLAGE_HJEMMEL VARCHAR2(100 CHAR);

comment on column KLAGE_VURDERING_RESULTAT_DVH.KLAGE_HJEMMEL IS 'Klage gjelder lovhjemmel';

update KLAGE_VURDERING_RESULTAT_DVH set KLAGE_HJEMMEL = '-' where KLAGE_HJEMMEL is null;
