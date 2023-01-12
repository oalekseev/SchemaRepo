package com.schemarepository.repository;

import com.schemarepository.model.Manual;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface JpaManualsRepository extends JpaRepository<Manual, Integer> {
    @Query("select new Manual(m.type) from Manual m group by m.type.id, m.type.name order by m.type.name asc")
    Page<Manual> findAllType(Pageable pageable);

    @Query("select new Manual(m.type) from Manual m where lower(m.type.name) like %?1% group by m.type.id, m.type.name order by m.type.name asc")
    Page<Manual> findAllTypeLike(String searchString, Pageable pageable);

    @Query("select new Manual(m.type, m.brand) from Manual m where m.type.name = ?1 group by m.type.id, m.brand.id, m.brand.name order by m.brand.name asc")
    Page<Manual> findAllBrandByType(String typeName, Pageable pageable);

    @Query("select new Manual(m.type, m.brand) from Manual m where m.type.name = ?1 and lower(m.brand.name) like %?2% group by m.type.id, m.brand.id, m.brand.name order by m.brand.name asc")
    Page<Manual> findAllBrandByTypeLike(String typeName, String searchString, Pageable pageable);

    @Query("select m from Manual m where m.type.name = ?1 and m.brand.name = ?2 order by m.modelName asc")
    Page<Manual> findAllModelByTypeAndBrand(String typeName, String brandName, Pageable pageable);

    @Query("select m from Manual m where m.type.name = ?1 and m.brand.name = ?2 and lower(m.modelName) like %?3% order by m.modelName asc")
    Page<Manual> findAllModelByTypeAndBrandLike(String typeName, String brandName, String searchString, Pageable pageable);
}
