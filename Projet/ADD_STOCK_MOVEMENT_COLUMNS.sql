-- Add missing columns to stock_movement table for ServiceConsommationService
ALTER TABLE stock_movement ADD COLUMN id_service INT(11) DEFAULT NULL AFTER id_stock_id;
ALTER TABLE stock_movement ADD COLUMN type_consommation VARCHAR(50) DEFAULT NULL AFTER type;
ALTER TABLE stock_movement ADD COLUMN reference_document VARCHAR(100) DEFAULT NULL AFTER motif;