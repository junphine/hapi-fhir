package com.satyy.ldap.model;

import java.io.Serializable;
import java.util.Objects;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The persistent class for the role database table.
 * 
 */

@Getter
@Setter
@NoArgsConstructor
public class Role implements Serializable {
	private static final long serialVersionUID = 1L;
	public static final String USER = "USER"; //普通用户
	public static final String ADMIN = "ADMIN"; //高级管理员
	public static final String MANAGER = "MANAGER";//中级管理员
	
	public static final String ROLE_USER = "ROLE_USER";
	public static final String ROLE_MANAGER = "ROLE_MANAGER";
	public static final String ROLE_ADMIN = "ROLE_ADMIN";

	@Id
	@Column(name = "ROLE_ID")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int roleId;

	@Column(name = "ROLE_NAME")
	private String name;

	@Column(name = "ROLE_KEY")
	private String key;

	public Role(String key,String name) {
		this.key = key;
		this.name = name;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		return Objects.equals(key, ((Role) obj).getName());
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("Role [key=").append(key).append("]").append("[id=").append(roleId).append("]");
		return builder.toString();
	}
}