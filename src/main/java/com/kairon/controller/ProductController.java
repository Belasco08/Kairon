package com.kairon.controller;

import com.kairon.dto.request.ProductRequest;
import com.kairon.dto.request.SellProductRequest;
import com.kairon.dto.response.ProductResponse;
import com.kairon.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal; // 👈 Importante

import java.util.List;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // 1. Criar Produto (Adicionado Principal)
    @PostMapping
    public ResponseEntity<ProductResponse> create(@RequestBody ProductRequest request, Principal principal) {
        // Agora passamos o request + o email do usuário logado
        return ResponseEntity.ok(productService.create(request, principal.getName()));
    }

    // Listar Produtos por Empresa
    @GetMapping
    public ResponseEntity<List<ProductResponse>> list(@RequestParam String companyId) {
        return ResponseEntity.ok(productService.listByCompany(companyId));
    }

    // 2. Atualizar Produto (Adicionado Principal)
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> update(@PathVariable String id, @RequestBody ProductRequest request, Principal principal) {
        // Agora passamos o ID + request + email do usuário logado
        return ResponseEntity.ok(productService.update(id, request, principal.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/sell")
    public ResponseEntity<Void> sell(@PathVariable String id, @RequestBody SellProductRequest request, Principal principal) {
        productService.sell(id, request, principal.getName());
        return ResponseEntity.ok().build();
    }
}