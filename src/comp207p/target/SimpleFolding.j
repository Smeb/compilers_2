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
	.limit stack 4

	getstatic java/lang/System/out Ljava/io/PrintStream;
	ldc2_w 67.32
  d2i
	ldc 12345
    iadd
    invokevirtual java/io/PrintStream/println(I)V
	return
.end method
