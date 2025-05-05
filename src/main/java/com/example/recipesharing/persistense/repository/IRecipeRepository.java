package com.example.recipesharing.persistense.repository;

import com.example.recipesharing.persistense.model.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IRecipeRepository extends JpaRepository<Recipe, Long> {

}
