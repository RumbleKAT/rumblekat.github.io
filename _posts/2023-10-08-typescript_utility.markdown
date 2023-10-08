---
layout: post
title:  "Typescript Utiltiy Type"
date:   2023-10-08 12:51:00 +0900
categories: dev
---

# 들어가면서 
Partial, Pick이라는 Utility 타입에 대해서 정리
새로 인터페이스를 늘리는 것이 아닌, 기존 인터페이스에서 원하는 부분만 뽑는게 Pick 
Partial의 경우, optional로 필드 전체를 적용 할수 있음


# Pick
~~~ typescript

interface Product{
    id: number;
    name: string;
    price: number;
    brand: string;
    stock: number;
}

// function fetchProducts(): Promise<Product[]> {
//
// }

// interface ProductDetail{
//     id: number;
//     name: string;
//     price: number;
// }

//2. 특정 상품의 상세 정보를 나타내기 위한 함수
type ShoppingItem = Pick<Product, 'id' | 'name' | 'price'>
function displayProductDetail(shoppingItem:ShoppingItem){
}
~~~

# Partial

~~~ typescript
interface UserProfile {
    username: string;
    email: string;
    profilePhotoUrl: string;
}

// interface UserProfileUpdate{
//     username?: string;
//     email?: string;
//     profilePhotoUrl?: string;
// }

// #1
type UserProfileUpdate = {
    username?: UserProfile['username'];
    email?: UserProfile['email'];
    profilePhotoUrl?: UserProfile['profilePhotoUrl'];
}

// #2
type UserProfileUpdate2 = {
    [p in 'username' | 'email' | 'profilePhotoUrl']?: UserProfile[p]
} //mapped Type

// #3
type UserProfileKeys = keyof UserProfile

type UserProfileUpdate3 = {
    [p in keyof UserProfile]?: UserProfile[p]
} //mapped Type

// #4
type Subset<T> = {
    [p in keyof T]?: T[p]
}

// const obj:Partial<UserProfile>
~~~
