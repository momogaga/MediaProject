/*
 Copyright (c) 2011 Michael Zucchi

 This file is part of socles, an OpenCL image processing library.

 socles is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 socles is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with socles.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 Hardcoded resampling code
 */
kernel void
downsample2_r(read_only image2d_t src, write_only image2d_t dst) {
	int2 pos = {get_global_id(0), get_global_id(1)};

//	if ((pos.x < get_image_width(dst)) & (pos.y < get_image_height(dst))) {
	const sampler_t smp = CLK_NORMALIZED_COORDS_FALSE | CLK_ADDRESS_CLAMP_TO_EDGE | CLK_FILTER_NEAREST;
//	const sampler_t smp = CLK_NORMALIZED_COORDS_FALSE | CLK_ADDRESS_NONE | CLK_FILTER_NEAREST;

	int2 spos = pos*2;
	float4 v = 0;

#pragma unroll
	for (int y=0;y<2;y++)
#pragma unroll
	for (int x=0;x<2;x++)
	v += read_imagef(src, smp, spos + (int2) {x, y});

	write_imagef(dst, pos, v * (1.0f/4));
//		write_imagef(dst, pos, v * (1.0f/1));
	//}
}

// TODO: better implementation of this one might be worth it.
kernel void
downsample4_r(read_only image2d_t src, write_only image2d_t dst) {
	int2 pos = {get_global_id(0), get_global_id(1)};

	if ((pos.x < get_image_width(dst)) & (pos.y < get_image_height(dst))) {
		const sampler_t smp = CLK_NORMALIZED_COORDS_FALSE | CLK_ADDRESS_CLAMP_TO_EDGE | CLK_FILTER_NEAREST;
		int2 spos = pos*4;
		float4 v = 0;
#pragma unroll
		for (int y=0;y<4;y++)
#pragma unroll
		for (int x=0;x<4;x++)
		v += read_imagef(src, smp, spos + (int2) {x, y});

		write_imagef(dst, pos, v * (1.0f/16));
	}
}

kernel void downsamplek_r(read_only image2d_t src, write_only image2d_t dst, int scale) {
	int2 pos = {get_global_id(0), get_global_id(1)};
	if ((pos.x < get_image_width(dst)) & (pos.y < get_image_height(dst))) {
		const sampler_t smp = CLK_NORMALIZED_COORDS_FALSE | CLK_ADDRESS_CLAMP_TO_EDGE | CLK_FILTER_LINEAR;
		int2 spos = pos*scale;
		float4 v = 0;
#pragma unroll
		for (int y=0;y<scale;y++) {
#pragma unroll
			for (int x=0;x<scale;x++) {
				v += read_imagef(src, smp, spos + (int2) {x, y});
			}
		}
		float gray=(0.2989 * v.x + 0.5870 * v.y + 0.1140 * v.z);
		v= (float4) {gray,gray,gray,0};
		write_imagef(dst, pos, v * (1.0f/(scale*scale)));
//		v=read_imagef(src, smp, spos);
//		write_imagef(dst, pos, v );
	}
}

kernel void downsamplek_float(read_only image2d_t src, write_only image2d_t dst, float scale) {
	//current pixel in dest image
	int2 pos = {get_global_id(0), get_global_id(1)};
	if ((pos.x < get_image_width(dst)) && (pos.y < get_image_height(dst))) {
		const sampler_t smp = CLK_NORMALIZED_COORDS_FALSE | CLK_ADDRESS_CLAMP_TO_EDGE | CLK_FILTER_LINEAR;
		int2 spos = (int2) {pos.x*scale, pos.y*scale};//pos*scale;
//		float2 spos = (pos.x*scale, pos.y*scale);
		float4 v = 0;
		//round scale to nearest int
		int iscale=ceil(scale);
#pragma unroll
    	for (int y=0;y<iscale;y++) {
#pragma unroll
			for (int x=0;x<iscale;x++) {
				v += read_imagef(src, smp, spos + (int2) {x, y});
			}
		}
 //   	float gray=(0.2989 * v.x + 0.5870 * v.y + 0.1140 * v.z);
  //  	v= (float4) {gray,gray,gray,0};
	//	write_imagef(dst, pos, v * (1.0f/(iscale*iscale)));
	//	v=read_imagef(src, smp, spos);
		write_imagef(dst, pos, v* (1.0f/(iscale*iscale)) );
	}
}


kernel void downsample(read_only image2d_t src, write_only image2d_t dst) {
	//current pixel in dest image
	int2 pos = {get_global_id(0), get_global_id(1)};
	if ((pos.x < get_image_width(dst)) & (pos.y < get_image_height(dst))) {

		float scaleX = 1.0f*get_image_width(src)/get_image_width(dst);
		float scaleY = 1.0f*get_image_height(src)/get_image_height(dst);

		const sampler_t smp = CLK_NORMALIZED_COORDS_FALSE | CLK_ADDRESS_CLAMP_TO_EDGE | CLK_FILTER_LINEAR;
		int2 spos = (int2) (pos.x*scaleX, pos.y*scaleY);//pos*scale;
//		float2 spos = (pos.x*scale, pos.y*scale);
		float4 v = 0;
		//round scale to nearest int
		int iscaleX=ceil(scaleX);
		int iscaleY=ceil(scaleY);

#pragma unroll
    	for (int y=0;y<iscaleY;y++) {
#pragma unroll
			for (int x=0;x<iscaleX;x++) {
				v += read_imagef(src, smp, spos + (int2) {x, y});
			}
		}
    	float gray=(0.2989 * v.x + 0.5870 * v.y + 0.1140 * v.z);
    	v= (float4) {gray,gray,gray,0};
	//	write_imagef(dst, pos, v * (1.0f/(iscale*iscale)));
	//	v=read_imagef(src, smp, spos);
		write_imagef(dst, pos, v* (1.0f/(iscaleX*iscaleY)) );
	}
}






