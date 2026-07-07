-- ---- bills table ----

-- Most common query: "get all bills for agency X between dates Y and Z"
-- This index covers both the WHERE agency_id = ? AND bill_date BETWEEN ? AND ?
-- AND the ORDER BY bill_date DESC — one index, all three clauses covered.
CREATE INDEX IF NOT EXISTS idx_bills_agency_date
    ON bills (agency_id, bill_date DESC);

-- Used by calculateStatus comparisons and the due-amount dashboard query
CREATE INDEX IF NOT EXISTS idx_bills_status
    ON bills (status);

-- Used by sumAllOutstandingDue() and countByDueAmountGreaterThan()
CREATE INDEX IF NOT EXISTS idx_bills_due_amount
    ON bills (due_amount);

-- ---- bill_items table ----

-- Every bill detail load does: SELECT * FROM bill_items WHERE bill_id = ?
-- Without this index, MySQL scans ALL items across ALL bills to find matches.
CREATE INDEX IF NOT EXISTS idx_bill_items_bill_id
    ON bill_items (bill_id);

-- ---- payments table ----

-- Every payment history load does: SELECT * FROM payments WHERE bill_id = ?
CREATE INDEX IF NOT EXISTS idx_payments_bill_id
    ON payments (bill_id);

-- ---- agencies table ----

-- Agency search by name (the search bar on the agencies page)
CREATE INDEX IF NOT EXISTS idx_agencies_name
    ON agencies (name);

-- Verify indexes were created:
SHOW INDEX FROM bills;
SHOW INDEX FROM bill_items;
SHOW INDEX FROM payments;