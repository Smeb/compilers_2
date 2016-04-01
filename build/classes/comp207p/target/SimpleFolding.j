; Jasmin Java assembler code that assembles the SimpleFolding example class

.source Type1.j
.class public comp207p/target/SimpleFolding
.super java/lang/Object

.method public <init>()V
	aload_0
	invokenonvirtual java/lang/Object/<init>()V
	return
.end method

.method public simple()V
	.limit stack 5

	getstatic java/lang/System/out Ljava/io/PrintStream;
	ldc 67
    i2d
	ldc 12345
    i2d
      dadd
      invokevirtual java/io/PrintStream/println(D)V
	return
.end method
