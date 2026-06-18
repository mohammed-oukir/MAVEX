package com.medafrica.mavex.security.annotation;

import com.medafrica.mavex.model.enums.PermissionAction;
import com.medafrica.mavex.model.enums.PermissionModule;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Vérifie en base de données que l'utilisateur connecté possède
 * la permission (module + action) avant d'exécuter la méthode.
 * Les ADMIN passent toujours. Les AGENT sans la permission reçoivent 403.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPermission {
    PermissionModule module();
    PermissionAction action();
}
