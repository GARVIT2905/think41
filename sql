-- 3. Tier-2: PostgreSQL full-text search setup
------------------------------------------------
-- 1) Add tsvector column and index:
ALTER TABLE workflows
  ADD COLUMN document tsvector GENERATED ALWAYS AS (
    to_tsvector('english', coalesce(name,'') || ' ' || coalesce(description,''))
  ) STORED;
CREATE INDEX workflows_document_idx ON workflows USING GIN(document);

-- 2) JPQL native query in repository:
-- WorkflowRepository.java
@Query(value = """
  SELECT w.* FROM workflows w
  WHERE w.document @@ plainto_tsquery(:q)
  ORDER BY ts_rank(w.document, plainto_tsquery(:q)) DESC
  """, nativeQuery = true)
List<Workflow> fullTextSearch(@Param("q") String query);
