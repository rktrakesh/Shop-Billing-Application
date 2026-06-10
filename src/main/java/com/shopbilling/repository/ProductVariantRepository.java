package com.shopbilling.repository;

import com.shopbilling.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    Optional<ProductVariant> findByBarcode(String barcode);
    Optional<ProductVariant> findByProductCode(String productCode);
    List<ProductVariant> findByProductId(Long productId);
    List<ProductVariant> findByStockLessThanEqualAndMinimumStockGreaterThan(int stock, int minimumStock);
    
    @Query("SELECT pv FROM ProductVariant pv WHERE pv.stock <= pv.minimumStock AND pv.active = true")
    List<ProductVariant> findLowStockVariants();
    
    @Query("SELECT pv FROM ProductVariant pv WHERE pv.active = true AND " +
           "(LOWER(pv.productCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(pv.color) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<ProductVariant> searchVariants(@Param("search") String search);
    
    boolean existsByBarcode(String barcode);
    boolean existsByProductCode(String productCode);
    
    @Query("SELECT COUNT(pv) FROM ProductVariant pv WHERE pv.stock <= pv.minimumStock AND pv.active = true")
    long countLowStockVariants();
}
