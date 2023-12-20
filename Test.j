.class public Test
.super java/lang/Object
.method public <init>()V
aload_0
invokenonvirtual java/lang/Object/<init>()V
return
.end method

.method public static add(III)I
.limit stack 32
.limit locals 32
iload 0
ldc 1
iadd 
istore 0
iload 0
iload 1
iload 2
iadd 
iadd 
ireturn
.end method

.method public static main([Ljava/lang/String;)V
.limit stack 32
.limit locals 32
ldc 1
istore 0
ldc 0
istore 1
iload 1
iload 0
ldc 2
invokestatic Test/add(III)I
istore 2
getstatic java/lang/System/out Ljava/io/PrintStream;
iload 2
invokestatic java/lang/String/valueOf(I)Ljava/lang/String;
invokevirtual java/io/PrintStream/println(Ljava/lang/String;)V
label_while:
iload 1
ldc 5
if_icmpge label_end
iload 2
ldc 4
if_icmpne label0
goto label0
goto label1
label0: 
goto label0
label1:
goto label_while
label_end:
getstatic java/lang/System/out Ljava/io/PrintStream;
iload 2
invokestatic java/lang/String/valueOf(I)Ljava/lang/String;
invokevirtual java/io/PrintStream/println(Ljava/lang/String;)V
return
.end method