# **Shift happens**

![][image1]  
**Group G**  
Members:  
Aleksandr Uktamovich Sorokin \- [also1002@stud.ek.dk](mailto:also1002@stud.ek.dk)  
Lucas Levy l'Espagnol Chantelou \- [luch0002@stud.ek.dk](mailto:luch0002@stud.ek.dk)  
Jafar Alsufaki \- [jaal0002@stud.ek.dk](mailto:jaal0002@stud.ek.dk)  
Sebastian Holger Drumm \- [sedr0001@stud.ek.dk](mailto:sedr0001@stud.ek.dk)  
Viggo Møhring Beck \- [vibe0002@stud.ek.dk](mailto:vibe0002@stud.ek.dk)

Project: Shift-Happens

Type: Shift scheduling system

Stack: Java 21, Spring Boot, MySQL, React \+ TypeScript

[**spørgsmål	4**](#spørgsmål)

[**Scope	5**](#heading=h.y74p7fn2xvrf)

[Functional Requirements	5](#heading=h.kkpuwg8zbf68)

[Shift swap request	6](#heading=h.lrxxr8kmg15p)

[Business rules	6](#heading=h.bhborukaaidp)

[Assumptions / Business Rules	6](#heading=h.5cpj6fwaf0xe)

[Approval Rules	8](#heading=h.oi2zkbjp0whb)

[CRUD Requirements	8](#heading=h.n7oh2zoxhjc3)

[Employee CRUD	9](#heading=h.gnb50shiq8lu)

[Department CRUD	9](#heading=h.6n3xdq4nfn6b)

[Work Location CRUD	10](#heading=h.vqnziyj53wif)

[Contract CRUD	10](#heading=h.c6nve5da3jyn)

[Job Role CRUD	11](#heading=h.n9tl2xc1v1mu)

[Suggested Unit Test Targets	11](#heading=h.ou0freibquc1)

[Non functional requirements	12](#heading=h.u6i8m638z89n)

[User Requirements	12](#heading=h.ts7qmtha5tzf)

[**Employes blackbox analyse	12**](#employes-blackbox-analyse)

# spørgsmål {#spørgsmål}

Har vi for mange tabeller? 

- Kan vi nøjes med mindre 

Mangler vi flere paritions linjer 

- F.eks. firstname /lastname, skal vi havde lange i strings med? Eller giver det ikke værdi?  

Do you want a SRS with comments on it? (Not talking about the SRS Review rapport but tha actual SRS)

# Employes blackbox analyse {#employes-blackbox-analyse}

**BVA and equivalence partitions for employee fields**

The minimum length for names is 1, for example the Danish name Ø.  
The minimum length for an e-mail is 5 because it needs to consist of a letter, followed by a @ and a domain. Minimum domain length is 3\. Example: a@a.dk

| Value | Partition type | Partitions | Test case values | Boundary values | Test case values |
| ----- | ----- | ----- | ----- | ----- | ----- |
| Email length | Invalid | 0-4 | 3 | 0 4 | 0 1 3 4 5 |
|  | Valid | 5 \- 320 | 150 | 5 320 | 4 5 6 319 320 321 |
| Password length | Invalid | 0-7 | 4 | 0 7 | 0 1 6 7 8 |
|  | Valid | 8-255 | 125 | 8 255 | 7 8 9 254 255 256 |
|  | Invalid | 256-MAX INT | 300 | 256 | 255 256 257 |
| First name \+ last name length | Invalid | 0 | 0 | 0 | 0 1 |
|  | Valid | 1-100 | 50 | 0 100 | 0 1 2 99 100 101 |
|  | Invalid | 101-MAX INT | 150 | 101 | 100 101 102 |

## First Name & last name {#first-name-&-last-name}

Starts with capital letter, 1 word, no whitespace

### Decision Table {#decision-table}

Minimum length name is based on the Danish name Ø.

| Case | Empty? | Letters only? | Result | Test value |
| :---- | :---- | :---- | :---- | :---- |
| 1 | No | Yes | Valid | Jensen |
| 2 | Yes | \- | Invalid | ““ |
| 3 | No | No (numbers) | Invalid | Jensen1 |
| 4 | No | No (symbols) | Invalid | Jensen@ |
| 5 | No | No (space) | Valid | Jensen Jens |

### Password black box tests {#password-black-box-tests}

The password must be at least 8 characters and can at most be 64 characters long.  
Must contain at least one of each:  
Lowercase letter, an uppercase letter, a number

###  **Decision table** {#decision-table-1}

| Case | Length 8 | Uppercase | Lowercase | Number | Test | Result |
| :---- | :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | False | \- | \- | \- | pass | Error |
| 2 | \- | False | \- | \- | password | Error |
| 3 | \- | \- | False | \- | PASSWORD | Error |
| 4 | \- | \- | \- | False | Password | Error |
| 5 | True | True | True | True | Passw0rd | Accepted |

**Email decision table:**

E-mails follow the format of length 320 and has a valid local part and domain, one @. We don’t verify the domain exists or the TLD. Just that the TLD is at least 2 letters long and the domain is at least one letter long.

* R0: Length is above 4 and below 321  
* R1: Contains exactly one @  
* R2: Valid local part format (Part prior to @)  
* R3: Valid domain format (at least 1 letter \+ includes dot \+ 2 letters)

| Case | R0 | R1 | R2 | R3 | Test | Result |
| :---- | :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | T | T | T | T | a@a.dk | Valid |
| 2 | \- | F | \- | \- | @@das | Invalid |
| 3 | F | \- | \- | \- | a@a | Invalid |
| 3 | \- | \- | F | \- | @aaa.dk | Invalid |
| 4 | \- | \- | \- | F | aaa@a | Invalid |
