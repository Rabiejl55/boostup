-- Ajoute une colonne d'archivage pour faire une "soft delete" des événements.
-- Safe: ne supprime rien, juste un flag.

ALTER TABLE evenement
    ADD COLUMN archived TINYINT(1) NOT NULL DEFAULT 0;

