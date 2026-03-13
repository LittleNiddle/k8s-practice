package com.beyond.order_system.product.controller;

import com.beyond.order_system.product.domain.Product;
import com.beyond.order_system.product.dto.*;
import com.beyond.order_system.product.service.ProductService;
import jakarta.persistence.Access;
import org.apache.coyote.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/product")
public class ProductController {
    private final ProductService productService;
    @Autowired
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping("/create")
    public ResponseEntity<?> create(@ModelAttribute ProductCreateReqDto dto, @RequestHeader("X-User-Email")String email){
        Long id = productService.create(dto, email);
        return ResponseEntity.status(HttpStatus.OK).body(id);
    }

    @GetMapping("/detail/{id}")
    public ResponseEntity<?> findById(@PathVariable Long id){
        ProductDetailResDto dto = productService.findById(id);
        return ResponseEntity.status(HttpStatus.OK).body(dto);
    }

    @GetMapping("/list")
    public ResponseEntity<?> findAll(@PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable, ProductSearchReqDto searchDto){
        Page<ProductListResDto> productListResDtoList = productService.findAll(searchDto, pageable);
        return ResponseEntity.status(HttpStatus.OK).body(productListResDtoList);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @ModelAttribute ProductUpdateReqDto dto){
        productService.update(id, dto);
        return ResponseEntity.status(HttpStatus.OK).body("");
    }

    @PutMapping("/updatestock")
    public ResponseEntity<?> updateStock(@RequestBody ProductStockUpdateDto dto){
        productService.updateStock(dto);
        return ResponseEntity.status(HttpStatus.OK).body("");
    }
}
