package com.shopbilling.repository;

import com.shopbilling.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByActiveTrue();
    List<Product> findByCategory(String category);
    List<Product> findByPrintType(String printType);
    
    @Query("SELECT DISTINCT p.category FROM Product p WHERE p.active = true")
    List<String> findAllCategories();
    
    @Query("SELECT DISTINCT p.printType FROM Product p WHERE p.active = true")
    List<String> findAllPrintTypes();
    
    @Query("SELECT p FROM Product p WHERE p.active = true AND " +
           "(LOWER(p.designName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.category) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Product> searchProducts(@Param("search") String search);
}
