# Excel Import (Phase 2 Implemented Slice)

## Current scope
- Admin import API supports:
	- Template download with required headers.
	- Preview upload with row-level validation and persisted preview results.
	- Confirm apply for valid previews only.
	- Downloadable error CSV for invalid rows.
	- Audit logging for preview and confirm actions.
	- Idempotent preview by optional idempotency key.

## Validation and safety rules
- File type must be `.xlsx` and file size <= 5MB.
- Required columns and ordering must match template.
- SKU must be unique within file and must not already exist.
- Import rows cannot set `Published=true`.
- Image URL must be `http` or `https` when provided.
- Confirm is blocked when any invalid row exists.

## Policy constraints
- Import must not silently overwrite sensitive fields.
- Import must not auto-publish newly imported products.
- Promotion/coupon updates require separate authorized flow.

## Remaining gaps for future phases
- Storage integration for image assets is not implemented (URL field only).
- Authentication and authorization are baseline-level and require hardening.
