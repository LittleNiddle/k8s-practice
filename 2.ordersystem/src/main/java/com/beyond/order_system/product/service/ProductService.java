package com.beyond.order_system.product.service;

import com.beyond.order_system.common.config.AwsS3Config;
import com.beyond.order_system.member.domain.Member;
import com.beyond.order_system.member.repository.MemberRepository;
import com.beyond.order_system.product.domain.Product;
import com.beyond.order_system.product.dto.*;
import com.beyond.order_system.product.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
public class ProductService {
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final S3Client s3Client;
    private final RedisTemplate<String ,String> redisTemplate;
    @Value("${aws.s3.bucket1}")
    private String bucket;

    public ProductService(ProductRepository productRepository, MemberRepository memberRepository, S3Client s3Client, @Qualifier("stockInventory") RedisTemplate<String, String> redisTemplate) {
        this.productRepository = productRepository;
        this.memberRepository = memberRepository;
        this.s3Client = s3Client;
        this.redisTemplate = redisTemplate;
    }

    public Long create(ProductCreateReqDto dto, String email){
        // repository 에서 error를 터뜨릴 수도 있다.
        Member member = memberRepository.findByEmail(email).orElseThrow(()->new NoSuchElementException("등록된 이메일이 없습니다."));
        Product product = dto.toEntity(member);
        Product productDb = productRepository.save(product);
        // multipart list??
        MultipartFile productImage = dto.getProductImage();
        if(productImage != null) {
            String filename = "product-" + product.getId() + "-productImage-" + productImage.getOriginalFilename();
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(filename)
                    .contentType(productImage.getContentType()) // image/jpeg, video/mp4, ...
                    .build();

            try {
                s3Client.putObject(request, RequestBody.fromBytes(productImage.getBytes()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            String imageUrl = s3Client.utilities().getUrl(a -> a.bucket(bucket).key(filename)).toExternalForm();
            product.updateProfileImageUrl(imageUrl);
        }
//        동시성 문제 해결을 위해 상품 등록 시 redis에 재고 생성
        redisTemplate.opsForValue().set(product.getId().toString(), product.getStockQuantity().toString());
        return productDb.getId();
    }

    public ProductDetailResDto findById(Long id){
        return ProductDetailResDto.fromEntity(productRepository.findById(id).orElseThrow(()->new NoSuchElementException("해당 상품이 없습니다.")));
    }

    public Page<ProductListResDto> findAll(ProductSearchReqDto searchDto, Pageable pageable){
        Specification<Product> specification = new Specification<Product>() {
            @Override
            public Predicate toPredicate(Root<Product> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();
                if(searchDto.getProductName()!=null) {
                    predicateList.add(criteriaBuilder.like(root.get("name"), "%"+searchDto.getProductName()+"%"));
                }
                if(searchDto.getCategory()!=null) {
                    predicateList.add(criteriaBuilder.equal(root.get("category"), searchDto.getCategory()));
                }

                Predicate[] predicateArr = new Predicate[predicateList.size()];
                for (int i = 0; i < predicateArr.length; i++) {
                    predicateArr[i] = predicateList.get(i);
                }
                Predicate predicate = criteriaBuilder.and(predicateArr);
                return predicate;
            }
        };

        Page<Product> productList = productRepository.findAll(specification, pageable);
        return productList.map(ProductListResDto::fromEntity);
    }

    public void update(Long id, ProductUpdateReqDto dto) {
        Product product = productRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("product cannot be found"));
        product.updateProduct(dto);
        if(dto.getProductImage()!=null){
//            이미지를 수정하거나 추가하고자 하는 경우 : 삭제 후 추가
//            기존 이미지를 파일 명으로 삭제
            if(product.getImage_path() != null){
                String imgUrl = product.getImage_path();
                String filename = imgUrl.substring(imgUrl.lastIndexOf("/")+1);
                s3Client.deleteObject(a->a.bucket(bucket).key(filename));
            }
//            신규 이미지를 등록
            String newFilename = "product-" + product.getId() + "-productImage-" + dto.getProductImage().getOriginalFilename();
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(newFilename)
                    .contentType(dto.getProductImage().getContentType()) // image/jpeg, video/mp4, ...
                    .build();

            try {
                s3Client.putObject(request, RequestBody.fromBytes(dto.getProductImage().getBytes()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            String imageUrl = s3Client.utilities().getUrl(a -> a.bucket(bucket).key(newFilename)).toExternalForm();
            product.updateProfileImageUrl(imageUrl);

        } else {
//            이미지를 삭제하고자 하는 경우
            if(product.getImage_path() != null){
                String imgUrl = product.getImage_path();
                String filename = imgUrl.substring(imgUrl.lastIndexOf("/")+1);
                s3Client.deleteObject(a->a.bucket(bucket).key(filename));
            }
        }
    }
}
