DROP ALL OBJECTS;

// Just an apple.
create table apple(
  id int primary key auto_increment
);

// Test references inside a partition.
create table pear(
  id int primary key auto_increment,
  apple_id int not null references apple
);

// Test references from the unpartitioned set to a partition.
create table knife(
  id int primary key auto_increment,
  pear_id int not null references pear,
  manufactured date not null
);

// Test references from one partition to the other partition.
create table bread(
  id int primary key auto_increment,
  apple_id int not null references apple
);

// Test references from a partition to the unpartitioned set.
create table pizza(
  id int primary key auto_increment,
  knife_id int not null references knife
);

// Test references from one partition to the other partition.
create table orange(
  id int primary key auto_increment,
  bread_id int not null references bread
);

insert into apple values (default);
insert into apple values (default);
insert into apple values (default);
insert into pear values (default, 1);
insert into knife values (default, 1, now());
insert into knife values (default, 1, now());
insert into bread values (default, 2);
insert into bread values (default, 2);
insert into pizza values (default, 1);
insert into pizza values (default, 2);
insert into pizza values (default, 2);
insert into orange values (default, 1);
insert into orange values (default, 1);
