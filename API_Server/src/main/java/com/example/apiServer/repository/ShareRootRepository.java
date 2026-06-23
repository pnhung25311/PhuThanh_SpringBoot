package com.example.apiServer.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.apiServer.model.file.ShareRoot;

import java.util.Optional;

public interface ShareRootRepository extends JpaRepository<ShareRoot, Long> {
    Optional<ShareRoot> findByAliasName(String aliasName);
}