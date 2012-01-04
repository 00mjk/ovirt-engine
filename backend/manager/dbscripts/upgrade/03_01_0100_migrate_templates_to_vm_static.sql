CREATE OR REPLACE FUNCTION Upgrade_MergeTemplatesToVmStatic_03_01_0090()
RETURNS void
AS $function$
DECLARE
   cur RECORD;
BEGIN

IF NOT EXISTS (SELECT * FROM information_schema.tables WHERE table_name ILIKE 'vm_templates') THEN
    RETURN;
END IF;

UPDATE vm_static
SET    entity_type = 'VM'
WHERE  vm_guid IN (SELECT vm_guid FROM vm_dynamic);

ALTER TABLE vm_static ALTER COLUMN entity_type SET NOT NULL;
ALTER TABLE vm_static DROP CONSTRAINT vm_templates_vm_static;
ALTER TABLE vm_static ADD CONSTRAINT vm_templates_vm_static FOREIGN KEY (vmt_guid) REFERENCES vm_static (vm_guid);

INSERT INTO vm_static (
    vm_guid,
    vmt_guid,
    vm_name,
    mem_size_mb,
    os,
    creation_date,
    child_count,
    num_of_sockets,
    cpu_per_socket,
    description,
    vds_group_id,
    domain,
    num_of_monitors,
    template_status,
    usb_policy,
    time_zone,
    is_auto_suspend,
    fail_back,
    vm_type,
    hypervisor_type,
    operation_mode,
    nice_level,
    default_boot_sequence,
    default_display_type,
    priority,
    auto_startup,
    is_stateless,
    iso_path,
    initrd_url,
    kernel_url,
    kernel_params,
    origin,
    _update_date,
    entity_type)
SELECT vmt_guid,
       '00000000-0000-0000-0000-000000000000',
       "name",
       mem_size_mb,
       os,
       creation_date,
       child_count,
       num_of_sockets,
       cpu_per_socket,
       description,
       vds_group_id,
       domain,
       num_of_monitors,
       status,
       usb_policy,
       time_zone,
       is_auto_suspend,
       fail_back,
       vm_type,
       hypervisor_type,
       operation_mode,
       nice_level,
       default_boot_sequence,
       default_display_type,
       priority,
       auto_startup,
       is_stateless,
       iso_path,
       initrd_url,
       kernel_url,
       kernel_params,
       origin,
       _update_date,
       'TEMPLATE'
FROM   vm_templates
WHERE  vmt_guid NOT IN (
    SELECT vm_guid
    FROM   vm_static
    WHERE  entity_type = 'TEMPLATE');

INSERT
INTO   image_vm_map(
    image_id,
    vm_id,
    active)
SELECT it_guid,
       vmt_guid,
       TRUE
FROM   vm_template_image_map
WHERE  it_guid NOT IN (
    SELECT image_id
    FROM   image_vm_map);

UPDATE image_templates it
SET    internal_drive_mapping =
    (SELECT internal_drive_mapping
     FROM   vm_template_image_map vtim
     WHERE  it.it_guid = vtim.it_guid)
WHERE  internal_drive_mapping IS NULL;

END; $function$
LANGUAGE plpgsql;


SELECT * FROM Upgrade_MergeTemplatesToVmStatic_03_01_0090();

DROP FUNCTION Upgrade_MergeTemplatesToVmStatic_03_01_0090();

